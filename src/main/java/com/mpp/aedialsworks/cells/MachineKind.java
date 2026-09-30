package com.mpp.aedialsworks.cells;
public enum MachineKind {
    IMPORT("import_interface",true,false,true,false), FLUID_IMPORT("fluid_import_interface",true,false,false,true),
    EXPORT("export_interface",false,true,true,false), FLUID_EXPORT("fluid_export_interface",false,true,false,true),
    COMBINED_IMPORT("combined_import_interface",true,false,true,true), COMBINED_EXPORT("combined_export_interface",false,true,true,true),
    ITEM_IO("item_io_interface",true,true,true,false), FLUID_IO("fluid_io_interface",true,true,false,true),
    EXPOSER("compacting_pattern_exposer",false,false,true,false), PROXY_FRONT("subnet_proxy_front",false,false,true,true), PROXY_BACK("subnet_proxy_back",false,false,true,true);
    public final String id;
    public final boolean input,output,items,fluids;
    MachineKind(String id,boolean input,boolean output,boolean items,boolean fluids){this.id=id;this.input=input;this.output=output;this.items=items;this.fluids=fluids;}
    public boolean resourceInterface(){return input||output;}
    public boolean proxy(){return this==PROXY_FRONT||this==PROXY_BACK;}
}
