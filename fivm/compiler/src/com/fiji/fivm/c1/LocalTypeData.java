/*
 * LocalTypeData.java
 * Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
 * This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
 * LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
 * available at fivm/LEGAL and can also be found at
 * http://www.fiji-systems.com/FPL3.txt
 * 
 * By installing, reproducing, distributing, and/or using the FIJI VM Software
 * you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
 * rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
 * restrictions stated therein.  Among other conditions and restrictions, the
 * FIJI PUBLIC LICENSE states that:
 * 
 * a. You may only make non-commercial use of the FIJI VM Software.
 * 
 * b. Any adaptation you make must be licensed under the same terms 
 * of the FIJI PUBLIC LICENSE.
 * 
 * c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
 * file, adaptation or output code that you distribute and cause the output code
 * to provide a notice of the FIJI PUBLIC LICENSE. 
 * 
 * d. You must not impose any additional conditions.
 * 
 * e. You must not assert or imply any connection, sponsorship or endorsement by
 * the author of the FIJI VM Software
 * 
 * f. You must take no derogatory action in relation to the FIJI VM Software
 * which would be prejudicial to the FIJI VM Software author's honor or
 * reputation.
 * 
 * 
 * The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
 * representation and provides no warranty of any kind concerning the software.
 * 
 * The FIJI PUBLIC LICENSE and any rights granted therein terminate
 * automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
 */

package com.fiji.fivm.c1;

import java.io.*;
import java.util.*;

import com.fiji.fivm.Settings;

public class LocalTypeData extends TypeData {
    Pointerable gcMap;
    
    public LocalTypeData(Type t) {
	super(t);
        try {
            assert !t.isBottomish();
            if (clazz().resolved() && !clazz().isAbstract()) {
                gcMap=Global.gcMapBuilder.buildGCMap(
                    new LinkableConstantAllocator(getName()),
                    clazz());
            } else {
                gcMap=Pointer.NULL;
            }
        } catch (Throwable e) {
            throw new CompilerException("Failed to create LocalTypeData for "+t,e);
        }
    }
    
    public boolean isLocal() { return true; }
    
    private VisibleClass clazz() {
        if (t.hasEffectiveClass()) {
            return t.effectiveClass();
        } else {
            return Global.root().objectClass;
        }
    }

    private String typedefName() {
	return getName()+"_type";
    }
    
    private String structName() {
	return typedefName()+"_s";
    }
    
    private int vtableLength() {
	if (clazz().unresolved() || clazz().isInterface() || !clazz().hasInstances()) {
	    return 0;
	} else {
	    return clazz().vtable.length;
	}
    }
    
    public void generateDeclaration(PrintWriter w) {
	if (vtableLength()==0) {
	    w.println("FIVMR_MAKE_TYPEDATA_NVT_S("+structName()+");");
	} else {
	    w.println("FIVMR_MAKE_TYPEDATA_S("+structName()+","+
		      vtableLength()+");");
	}
	w.println("typedef struct "+structName()+" "+typedefName()+";");
	w.println(typedefName()+" "+getName()+";");
    }

    public void generateDefinition(PrintWriter w) {
	try {
            assert t.resolved();
            
	    String bucketsName=getName()+"_buckets";
	    String methodsName=getName()+"_methods";
	    String fieldsName=getName()+"_fields";
	    String itableName=getName()+"_itable";
	    String interfacesName=getName()+"_interfaces";
            String directSubsName=getName()+"_directSubs";
            String ilistName=getName()+"_ilist";
	    
	    int numMethods=0;
	    int numFields=0;
	
	    w.println(typedefName()+" "+getName()+";");
	
	    if (t.resolved() && t.isObject()) {
		w.println("static int8_t "+bucketsName+"["+t.buckets.length+"] = {");
		for (int i=0;i<t.buckets.length;++i) {
		    w.print("   INT8_C("+t.buckets[i]+")");
		    if (i<t.buckets.length-1) {
			w.println(",");
		    } else {
			w.println();
		    }
		}
		w.println("};");
	    }
	
	    if (t.hasConcreteSupertypes()) {
                Type[] superinterfaces=t.superInterfaces();
		
		if (superinterfaces.length>0) {
		    w.println("static fivmr_TypeData *"+interfacesName+"["+
			      superinterfaces.length+"] = {");
		    for (int i=0;i<superinterfaces.length;++i) {
			w.print("   (fivmr_TypeData*)&"+forType(superinterfaces[i]).getName());
			if (i==superinterfaces.length-1) {
			    w.println();
			} else {
			    w.println(",");
			}
		    }
		    w.println("};");
		}
                
            }
            
            if (t.resolved() && Settings.TRACK_DIRECT_SUBS) {
                ArrayList< Type > directSubs=t.directUsedSubtypes();
                
                if (directSubs.size()>0) {
                    w.println("static fivmr_TypeData *"+directSubsName+"["+
                              directSubs.size()+"] = {");
                    for (int i=0;i<directSubs.size();++i) {
                        w.print("   (fivmr_TypeData*)&"+forType(directSubs.get(i)).getName());
			if (i==directSubs.size()-1) {
			    w.println();
			} else {
			    w.println(",");
			}
                    }
		    w.println("};");
                }
	    }
            
            HashSet< VisibleClass > ilist=new HashSet< VisibleClass >();
            if (t.resolved() && t.isObject() && !t.isArray()) {
                VisibleClass c=t.effectiveClass();
                if (c.isInterface()) {
                    for (VisibleClass c2 : c.allStrictSupertypes()) {
                        if (c2.isInterface()) {
                            ilist.add(c2);
                        }
                    }
                } else {
                    VisibleClass c2=c.getSuperclass();
                    for (VisibleClass c3 : c.allStrictSupertypes()) {
                        if (c3.isInterface() &&
                            (c2==null || !c2.allStrictSupertypes().contains(c3))) {
                            ilist.add(c3);
                        }
                    }
                }
                
                if (ilist.size()>0) {
                    w.println("static fivmr_TypeData *"+ilistName+"["+
                              ilist.size()+"] = {");
                    boolean first=true;
                    for (VisibleClass c2 : ilist) {
                        if (first) {
                            first=false;
                            w.print("   ");
                        } else {
                            w.print(",  ");
                        }
                        w.println("(fivmr_TypeData*)&"+forType(c2.asType()).getName());
                    }
                    w.println("};");
                }
            }
	    
	    if (t.resolved() && t.mapsDirectlyToClass()) {
		VisibleClass c=t.effectiveClass();

		numFields=c.numExistingFields();
		if (numFields>0) {
		    w.println("static fivmr_FieldRec "+fieldsName+"["+numFields+
			      "] = {");
		    boolean first=true;
		    for (VisibleField f : c.fields()) {
			if (!f.shouldExist()) {
			    continue;
			}
			if (first) {
			    w.print("   ");
			    first=false;
			} else {
			    w.print(",  ");
			}
			w.print("{ (fivmr_TypeData*)&"+getName()+", \""+f.getName()+"\", ");
			w.print(f.runtimeFlags());
			w.print(", (fivmr_TypeStub*)"+TypeStub.forType(f.getType()).asCCode()+", ");
			if (f.isStatic()) {
                            w.print("(uintptr_t)("+StaticFieldRepo.offsetForField(f).asCCode()+")");
			} else {
			    w.print("(uintptr_t)"+f.location);
			}
			w.println(" }");
		    }
		    w.println("};");
		}
		
		numMethods=c.numExistingMethods();
		if (numMethods>0) {
		    w.println("static fivmr_MethodRec *"+methodsName+"["+numMethods+
			      "] = {");
		    boolean first=true;
		    for (VisibleMethod m : c.methods()) {
			if (!m.shouldExist()) {
			    continue;
			}
			if (first) {
			    w.print("   ");
			    first=false;
			} else {
			    w.print(",  ");
			}
			w.println("&"+MethodRec.linkageName(m));
		    }
		    w.println("};");
		}
	    }
	
	    if (t.resolved() &&
		!clazz().isInterface() && clazz().hasInstances() &&
		clazz().maxITableIndex>=clazz().minITableIndex) {
		w.println("static void *"+itableName+"["+(
			      clazz().maxITableIndex-clazz().minITableIndex+1)+
			  "] = {");
		for (int i=clazz().minITableIndex;
		     i<=clazz().maxITableIndex;
		     ++i) {
		    w.print("   ");
		    if (clazz().itable[i]==null ||
			!clazz().itable[i].shouldHaveCode()) {
			w.print("NULL");
		    } else {
			w.print("&"+clazz().itable[i].asRemoteFunction().getName());
		    }
		    if (i<clazz().maxITableIndex) {
			w.println(",");
		    } else {
			w.println();
		    }
		}
		w.println("};");
	    }
	
	    w.println(typedefName()+" "+getName()+" = {");
	    w.println("   FIVMR_MS_INVALID,");
	    w.println("   (fivmr_TypeData*)&"+getName()+",");
	    w.println("   "+t.runtimeFlags()+",");
	    w.println("   \""+Util.cStringEscape(t.jniName())+"\",");
            w.println("   "+Global.name+"_contexts+"+t.getContext().runtimeIndex()+","); // context
	    w.println("   (int32_t)"+t.valueOfRuntimeInitedField()+","); // inited
	    w.println("   NULL,"); // cur initer
	    if (t.resolved() && t.hasClass()) {
		if (t.getClazz().sourceFilename!=null) {
		    w.println("   \""+Util.cStringEscape(t.getClazz().sourceFilename)+"\",");
		} else {
		    w.println("   NULL,");
		    if (Global.verbosity>=2) {
			Global.log.println("Warning: "+t+" doesn't have a source filename.");
		    }
		}
	    } else {
		w.println("   NULL,");
	    }
	    if (t.hasConcreteSupertypes()) {
		w.println("   (fivmr_TypeData*)&"+forType(t.supertype()).getName()+",");
	    } else {
		w.println("   NULL,");
	    }
	    if (t.hasConcreteSupertypes()) {
                Type[] superinterfaces=t.superInterfaces();
                ArrayList< Type > directSubs=t.directUsedSubtypes();
		w.println("   (uint16_t)"+superinterfaces.length+",");
                w.println("   (uint16_t)"+directSubs.size()+",");
                w.println("   (uint16_t)"+ilist.size()+",");
		if (superinterfaces.length>0) {
		    w.println("   "+interfacesName+",");
		} else {
		    w.println("   NULL,");
		}
                if (ilist.size()>0) {
                    w.println("   "+ilistName+",");
                } else {
                    w.println("   NULL,");
                }
                if (directSubs.size()>0 && Settings.TRACK_DIRECT_SUBS) {
                    w.println("   "+directSubsName+",");
                } else {
                    w.println("   NULL,"); // known direct subs
                }
	    } else {
		w.println("   (uint16_t)0,");
		w.println("   (uint16_t)0,");
		w.println("   (uint16_t)0,");
		w.println("   NULL,");
		w.println("   NULL,");
                w.println("   NULL,");
	    }
            if (t.resolved() && t.hasClass()) {
		w.println("   (int32_t)"+t.getClazz().canonicalNumber+",");
            } else {
		w.println("   (int32_t)0,");
            }
            w.println("   (int32_t)"+t.numDescendants+",");
            w.println("   {"); // epochs
            w.println("      {"); // epochs[0]
            // generate two identical epochs
            for (int i=0;i<2;++i) {
                if (t.resolved() &&
                    !clazz().isInterface() && clazz().hasInstances() &&
                    clazz().maxITableIndex>=clazz().minITableIndex) {
                    w.println("         (uint16_t)"+clazz().minITableIndex+",");
                    w.println("         (uint16_t)"+(clazz().maxITableIndex-clazz().minITableIndex+1)+",");
                    w.println("         "+itableName+"-"+clazz().minITableIndex+",");
                } else {
                    w.println("         (uint16_t)0,");
                    w.println("         (uint16_t)0,");
                    w.println("         NULL,");
                }
                if (t.resolved() && t.isObject()) {
                    w.println("         "+bucketsName+",");
                } else {
                    w.println("         NULL,");
                }
                w.println("         (int8_t)"+t.tid+",");
                w.println("         (uint16_t)"+(int)t.bucket+",");
                if (i==0) {
                    w.println("      }, {");
                }
            }
            w.println("      }");
            w.println("   },");
	    if (t.isArray()) {
		w.println("   (fivmr_TypeData*)&"+forType(t.arrayElement()).getName()+",");
	    } else {
		w.println("   NULL,");
	    }
            if (t.arrayTypeCreated() &&
                t.arrayTypeIfCreated().isUsed() &&
                t.arrayTypeIfCreated().resolved()) {
                w.println("   (fivmr_TypeData*)&"+forType(t.arrayTypeIfCreated()).getName()+",");
            } else {
                w.println("   NULL,");
            }
	    if (clazz().resolved()) {
		w.println("   (int32_t)"+clazz().alignedPayloadSize()+",");
                w.println("   (int8_t)"+(clazz().alignedPayloadSize()-clazz().payloadSize())+",");
		w.println("   (int8_t)"+clazz().requiredPayloadAlignment()+",");
	    } else {
		w.println("   (int32_t)0,");
		w.println("   (int8_t)0,");
		w.println("   (int8_t)0,");
	    }
	    if (t.effectiveBasetype().isUsableType) {
		w.println("   (int8_t)"+t.effectiveBasetype().bytes+",");
	    } else {
		w.println("   (int8_t)0,");
	    }
            if (Settings.CLASSLOADING && t.mapsDirectlyToClass()) {
                w.println("   (fivmr_Object)("+BytecodeRepository.pointerTo(
                              t.effectiveClass()).asCCode()+"),");
            } else {
                w.println("   0,"); // bytecode
            }
	    w.println("   (fivmr_Object)((uintptr_t)("+ClassRepository.pointerTo(t).asCCode()+")),");
            w.println("   NULL,"); // TypeDataNode* node
	    if (t.resolved() && t.mapsDirectlyToClass()) {
		w.println("   (uint16_t)"+numMethods+",");
		w.println("   (uint16_t)"+numFields+",");
		if (numMethods==0) {
		    w.println("   NULL,");
		} else {
		    w.println("   "+methodsName+",");
		}
		if (numFields==0) {
		    w.println("   NULL,");
		} else {
		    w.println("   "+fieldsName+",");
		}
	    } else {
		w.println("   (uint16_t)0,");
		w.println("   (uint16_t)0,");
		w.println("   NULL,");
		w.println("   NULL,");
	    }
	    w.println("   (uintptr_t)("+gcMap.asCCode()+"),");
	    w.println("   (int32_t)"+t.uniqueID+",");
            w.println("   (int32_t)"+vtableLength()+",");
	    // FIXME: we're currently outputting a vtable for primitives... that's
	    // not wrong, I guess, but it's stupid.
	    if (vtableLength()>0) {
		w.print("   { ");
		for (int i=0;i<vtableLength();++i) {
		    if (clazz().vtable[i]!=null &&
			clazz().vtable[i].shouldHaveCode()) {
			w.print("&"+clazz().vtable[i].asRemoteFunction().getName());
		    } else {
			w.print("NULL");
		    }
		    if (i<vtableLength()-1) {
			w.print(", ");
		    }
		}
		w.println(" }");
	    }
	    w.println("};");
	} catch (Throwable e) {
	    throw new CompilerException("Failed to generate local type data definition for "+t,e);
	}
    }
    
    public LinkableSet subLinkables() {
	LinkableSet result=new LinkableSet();
        result.add(CTypesystemReferences.generated_contexts);
	result.add(ClassRepository.pointerTo(t));
	result.add(gcMap);
	if (t.resolved() && t.hasConcreteSupertypes()) {
	    result.add(forType(t.supertype()));
	}
	if (t.isArray()) {
	    result.add(forType(t.arrayElement()));
	}
	result.add(forType(t.arrayBase()));
	if (t.resolved() && t.hasClass()) {
	    VisibleClass c=t.getClazz();
	    for (VisibleClass c2 : c.getSuperInterfaces()) {
		result.add(forType(c2.asType()));
	    }
	}
	if (t.resolved() && t.mapsDirectlyToClass()) {
	    VisibleClass c=t.effectiveClass();
	    // fields and methods
	    for (VisibleField f : c.fields()) {
		if (f.shouldExist()) {
		    result.add(TypeStub.forType(f.getType()));
		}
	    }
	    for (VisibleMethod m : c.methods()) {
		if (m.shouldExist()) {
		    result.add(new LocalMethodRec(m));
		}
	    }
	}
        if (Settings.CLASSLOADING && t.mapsDirectlyToClass()) {
            result.add(BytecodeRepository.pointerTo(t.effectiveClass()));
        }
	if (t.resolved() && !clazz().isInterface() && clazz().hasInstances()) {
	    for (VisibleMethod m : clazz().itable) {
		if (m!=null && m.shouldHaveCode()) {
		    result.add(m.asRemoteFunction());
		}
	    }
	    for (VisibleMethod m : clazz().vtable) {
		if (m!=null && m.shouldHaveCode()) {
		    result.add(m.asRemoteFunction());
		}
	    }
	}
	return result;
    }
}

