import java.io.*;

/**
 * ClassFileWriter
 *
 * A ClassFileWriter is used to write a Java class file. Methods are
 * provided to create fields and methods, and within methods to write
 * Java bytecodes.
 *
 * @author Roger Lawrence
 */
public class ClassFileWriter {
    
    private static final int LineNumberTableSize = 16;
    private static final int ExceptionTableSize = 4;

    private final static long FileHeaderConstant = 0xCAFEBABE0003002DL;
    // Set DEBUG flags to true to get better checking and progress info.
    private static final boolean DEBUGSTACK = false;
    private static final boolean DEBUGLABELS = false;
    private static final boolean DEBUGCODE = false;

    private String generatedClassName;

    private ExceptionTableEntry itsExceptionTable[];
    private int itsExceptionTableTop;

    private int itsLineNumberTable[];   // pack start_pc & line_number together
    private int itsLineNumberTableTop;

    private byte[] itsCodeBuffer = new byte[256];
    private int itsCodeBufferTop;

    private ConstantPool itsConstantPool;

    private ClassFileMethod itsCurrentMethod;
    private short itsStackTop;

    private short itsMaxStack;
    private short itsMaxLocals;

    private short itsFlags;
    private short itsThisClassIndex;
    private short itsSuperClassIndex;
    private short itsSourceFileNameIndex;

    private static final int MIN_LABEL_TABLE_SIZE = 32;
    private int[] itsLabelTable;
    private int itsLabelTableTop;

// itsFixupTable[i] = (label_index << 32) | fixup_site
    private static final int MIN_FIXUP_TABLE_SIZE = 40;
    private long[] itsFixupTable;
    private int itsFixupTableTop;

    private char[] tmpCharBuffer = new char[64];
}

final class ExceptionTableEntry
{

    ExceptionTableEntry(int startLabel, int endLabel,
                        int handlerLabel, short catchType)
    {
        itsStartLabel = startLabel;
        itsEndLabel = endLabel;
        itsHandlerLabel = handlerLabel;
        itsCatchType = catchType;
    }

    int itsStartLabel;
    int itsEndLabel;
    int itsHandlerLabel;
    short itsCatchType;
}

final class ClassFileField
{

    ClassFileField(short nameIndex, short typeIndex, short flags)
    {
        itsNameIndex = nameIndex;
        itsTypeIndex = typeIndex;
        itsFlags = flags;
        itsHasAttributes = false;
    }

    private short itsNameIndex;
    private short itsTypeIndex;
    private short itsFlags;
    private boolean itsHasAttributes;
    private short itsAttr1, itsAttr2, itsAttr3;
    private int itsIndex;
}

final class ClassFileMethod
{

    ClassFileMethod(short nameIndex, short typeIndex, short flags)
    {
        itsNameIndex = nameIndex;
        itsTypeIndex = typeIndex;
        itsFlags = flags;
    }

    private short itsNameIndex;
    private short itsTypeIndex;
    private short itsFlags;
    private byte[] itsCodeAttribute;

}

final class ConstantPool
{

    ConstantPool(ClassFileWriter cfw)
    {
        this.cfw = cfw;
        itsTopIndex = 1;       // the zero'th entry is reserved
        itsPool = new byte[ConstantPoolSize];
        itsTop = 0;
    }

    private static final int ConstantPoolSize = 256;
    private ClassFileWriter cfw;

    private static final int MAX_UTF_ENCODING_SIZE = 65535;

    private int itsTop;
    private int itsTopIndex;
    private byte itsPool[];
}

final class FieldOrMethodRef
{
    FieldOrMethodRef(String className, String name, String type)
    {
        this.className = className;
        this.name = name;
        this.type = type;
    }

    private String className;
    private String name;
    private String type;
    private int hashCode = -1;
}
