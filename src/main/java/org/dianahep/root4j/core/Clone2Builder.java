package org.dianahep.root4j.core;


import org.dianahep.root4j.RootClass;
import org.dianahep.root4j.RootMember;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;


/**
 * Builds a class which extends Clone and represents one item in a TClonesArray.
 * This is used when the TClonesArray is read in split mode.
 * Each class contains its index within the TClonesArray, and a pointer to the
 * corresponding class which extends Clones.
 * Clone2Builder is used for Root 3.01 and later.
 * @see org.dianahep.root4j.core.Clones2Builder
 * @author tonyj
 * @version $Id: Clone2Builder.java 13619 2009-04-10 00:28:43Z tonyj $
 */
class Clone2Builder implements ClassBuilder, Constants
{
   private static NameMangler nameMangler = NameMangler.instance();
   private static boolean optimize;

   public String getStem()
   {
      return "org.dianahep.root4j.clone2";
   }

   public JavaClass build(GenericRootClass klass)
   {
      optimize = (klass.getStreamerInfo().getBits() & (1 << 12)) == 0;

      String className = nameMangler.mangleFullClassName(getStem(),klass.getClassName());
      String clonesClassName = nameMangler.mangleFullClassName("org.dianahep.root4j.clones2",klass.getClassName());
      ClassGen cg = new ClassGen(className, "org/dianahep/root4j/core/Clone2", "<generated>", ACC_PUBLIC | ACC_SUPER, new String[]
         {
            nameMangler.mangleInterfaceName(klass.getClassName())
         });
      ConstantPoolGen cp = cg.getConstantPool();
      InstructionList il = new InstructionList();
      InstructionFactory factory = new InstructionFactory(cg);

      cg.addEmptyConstructor(ACC_PUBLIC);

      // Build the complete list of superclasses
      List sup = new ArrayList();
      RootClass[] superClasses = klass.getSuperClasses();
      iterativelyAdd(sup, superClasses);
      sup.add(klass);

      // Create the fields
      cg.addField(new FieldGen(ACC_PRIVATE, Type.INT, "index", cp).getField());
      cg.addField(new FieldGen(ACC_PRIVATE, new ObjectType(clonesClassName), "clones", cp).getField());

      //Generate the setData method
      MethodGen mg = new MethodGen(ACC_PUBLIC, Type.VOID, new Type[]
         {
            Type.INT, new ObjectType("org.dianahep.root4j.core.Clones2")
         }, new String[] { "index", "clones" }, "setData", className, il, cp);
      il.append(InstructionConstants.ALOAD_0);
      il.append(InstructionConstants.ILOAD_1);
      il.append(factory.createPutField(className, "index", Type.INT));
      il.append(InstructionConstants.ALOAD_0);
      il.append(InstructionConstants.ALOAD_2);
      il.append(factory.createCast(new ObjectType("org.dianahep.root4j.core.Clones2"), new ObjectType(clonesClassName)));
      il.append(factory.createPutField(className, "clones", new ObjectType(clonesClassName)));
      il.append(InstructionConstants.RETURN);
      mg.setMaxStack();
      mg.setMaxLocals();
      cg.addMethod(mg.getMethod());
      il.dispose();

      // Generate the accessor methods
      for (Iterator i = sup.iterator(); i.hasNext();)
         generateMethods((RootClass) i.next(), cp, il, factory, cg, className, clonesClassName);

      return cg.getJavaClass();
   }

   private static void generateMethods(RootClass k, ConstantPoolGen cp, InstructionList il, InstructionFactory factory, ClassGen cg, String className, String clonesClassName)
   {
      RootMember[] members = k.getMembers();

      for (int i = 0; i < members.length; i++)
      {
         BasicMember member = (BasicMember) members[i];

         Type type = member.getJavaType();
         Type arrayType = new ArrayType(type, 1);
         MethodGen mg = new MethodGen(ACC_PUBLIC, type, null, null, nameMangler.mangleMember(member.getName()), className, il, cp);
         il.append(InstructionConstants.ALOAD_0);
         il.append(factory.createGetField(className, "clones", new ObjectType(clonesClassName)));
         il.append(InstructionConstants.ALOAD_0);
         il.append(factory.createGetField(className, "index", Type.INT));

         il.append(factory.createInvoke(clonesClassName, nameMangler.mangleMember(member.getName()), type, new Type[]
               {
                  Type.INT
               }, INVOKEVIRTUAL));

         il.append(InstructionFactory.createReturn(type));
         mg.setMaxStack();
         mg.setMaxLocals();
         cg.addMethod(mg.getMethod());
         il.dispose();
      }
   }

   private void iterativelyAdd(List list, RootClass[] superClasses)
   {
      for (int i = 0; i < superClasses.length; i++)
      {
         RootClass[] supsup = superClasses[i].getSuperClasses();
         iterativelyAdd(list, supsup);
         list.add(superClasses[i]);
      }
   }
}
