package com.mpp.aedialsworks.cellterminal.network;
import java.io.*;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BulkProtocolTest {
    private static CompoundTag snapshot(long... ids){var tag=new CompoundTag();var list=new ListTag();for(long id:ids){var entry=new CompoundTag();entry.putLong("id",id);entry.putLong("amount",Long.MAX_VALUE-id);list.add(entry);}tag.put("entries",list);return tag;}
    @Test void deltaAddsUpdatesRemovesAndPreservesLongs(){
        var tracker=new DeltaSnapshot();var first=tracker.prepare(TerminalChannels.STORAGES,snapshot(1,2),true);assertTrue(first.full());tracker.commit(TerminalChannels.STORAGES,first);
        var next=snapshot(2,3);next.getList("entries",Tag.TAG_COMPOUND).getCompound(0).putLong("amount",42);
        var delta=tracker.prepare(TerminalChannels.STORAGES,next,true);assertFalse(delta.full());assertEquals(next,DeltaSnapshot.apply(first.payload(),delta.payload(),false));tracker.commit(TerminalChannels.STORAGES,delta);assertNull(tracker.prepare(TerminalChannels.STORAGES,next,true));
        assertTrue(tracker.prepare(TerminalChannels.BUSES,next,true).full());tracker.reset();assertTrue(tracker.prepare(TerminalChannels.STORAGES,next,true).full());
    }
    @Test void snapshotDoesNotAdvanceUntilEncodedAndCommitted(){var tracker=new DeltaSnapshot();var a=tracker.prepare("ct:temp",snapshot(1),true);a.payload().putString("mutable","yes");assertTrue(tracker.prepare("ct:temp",snapshot(2),true).full());tracker.commit("ct:temp",a);assertFalse(tracker.prepare("ct:temp",snapshot(2),true).full());}
    @Test void duplicateIdsAndDeltaBeforeFullAreRejected(){assertThrows(IllegalArgumentException.class,()->DeltaSnapshot.entries(snapshot(1,1)));assertThrows(IllegalStateException.class,()->DeltaSnapshot.apply(null,new CompoundTag(),false));}
    @Test void chunksReassembleOutOfOrderAndIgnoreDuplicates()throws Exception{
        var tag=snapshot(1,2);byte[] noise=new byte[300000];new Random(4).nextBytes(noise);tag.putByteArray("noise",noise);
        var encoded=ChunkCodec.encode(tag,4096);assertTrue(encoded.size()>1);var assembler=new ChunkAssembler();var session=UUID.randomUUID();Optional<ChunkAssembler.Payload> result=Optional.empty();
        for(int i=encoded.size()-1;i>=0;i--){var p=new PacketNBTChunk(7,session,"ct:storages",1,true,i,encoded.size(),encoded.get(i));result=assembler.accept(p,10);assertTrue(assembler.accept(p,10).isEmpty());}
        assertEquals(tag,result.orElseThrow().tag());assertTrue(assembler.accept(new PacketNBTChunk(7,session,"ct:storages",1,true,0,encoded.size(),encoded.get(0)),11).isEmpty());
    }
    @Test void capFramesEvenWhenConfigRequestsTenMegabytes()throws Exception{var tag=new CompoundTag();byte[] noise=new byte[300000];new Random(1).nextBytes(noise);tag.putByteArray("noise",noise);for(var frame:ChunkCodec.encode(tag,10485760))assertTrue(frame.length<=ChunkCodec.MAX_FRAME);}
    @Test void rejectInvalidHeadersAndCorruptCompression(){var id=UUID.randomUUID();assertThrows(IllegalArgumentException.class,()->new PacketNBTChunk(1,id,"unknown",1,true,0,1,new byte[]{1}));assertThrows(IllegalArgumentException.class,()->new PacketNBTChunk(1,id,"ct:temp",1,true,2,2,new byte[]{1}));assertThrows(IllegalArgumentException.class,()->new PacketNBTChunk(1,id,"ct:temp",1,true,0,Integer.MAX_VALUE,new byte[]{1}));assertThrows(IOException.class,()->ChunkCodec.decode(new byte[]{1,2,3}));}
    @Test void channelsAreIndependentAndMetadataMustMatch()throws Exception{var a=new ChunkAssembler();var id=UUID.randomUUID();a.accept(new PacketNBTChunk(1,id,"ct:temp",1,true,0,2,new byte[]{1}),0);assertThrows(IOException.class,()->a.accept(new PacketNBTChunk(1,id,"ct:temp",1,false,1,2,new byte[]{1}),0));var tag=snapshot(9);var frame=ChunkCodec.encode(tag,4096).get(0);assertEquals(tag,a.accept(new PacketNBTChunk(1,id,"ct:buses",1,true,0,1,frame),0).orElseThrow().tag());a.close();}
}
