package local.classloader;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ClassFileInfo {
    private static final Pattern DESCRIPTOR_TYPE = Pattern.compile("L([A-Za-z0-9_$/]+)(?:;|<)");

    final String binaryName;
    final int majorVersion;
    final Set<String> referencedClasses;

    private ClassFileInfo(String binaryName, int majorVersion, Set<String> referencedClasses) {
        this.binaryName = binaryName;
        this.majorVersion = majorVersion;
        this.referencedClasses = Collections.unmodifiableSet(referencedClasses);
    }

    static ClassFileInfo parse(byte[] bytes) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != 0xCAFEBABE) {
                throw new IOException("invalid class magic");
            }
            input.readUnsignedShort();
            int major = input.readUnsignedShort();
            if (major < 45 || major > 52) {
                throw new IOException("unsupported class-file version " + major + " (Java 8 supports up to 52)");
            }

            int count = input.readUnsignedShort();
            Object[] pool = new Object[count];
            int[] tags = new int[count];
            for (int i = 1; i < count; i++) {
                int tag = input.readUnsignedByte();
                tags[i] = tag;
                switch (tag) {
                    case 1:
                        pool[i] = input.readUTF();
                        break;
                    case 3:
                    case 4:
                        skip(input, 4);
                        break;
                    case 5:
                    case 6:
                        skip(input, 8);
                        i++;
                        break;
                    case 7:
                    case 8:
                    case 16:
                    case 19:
                    case 20:
                        pool[i] = input.readUnsignedShort();
                        break;
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 17:
                    case 18:
                        skip(input, 4);
                        break;
                    case 15:
                        skip(input, 3);
                        break;
                    default:
                        throw new IOException("unknown constant-pool tag " + tag);
                }
            }

            input.readUnsignedShort();
            int thisClassIndex = input.readUnsignedShort();
            if (thisClassIndex <= 0 || thisClassIndex >= count || tags[thisClassIndex] != 7) {
                throw new IOException("invalid this_class entry");
            }
            int nameIndex = (Integer) pool[thisClassIndex];
            String internalName = (String) pool[nameIndex];
            if (internalName == null || internalName.length() == 0 || internalName.charAt(0) == '[') {
                throw new IOException("invalid class name");
            }
            if (internalName.startsWith("java/")) {
                throw new IOException("classes in java.* packages are not loadable");
            }

            Set<String> references = new LinkedHashSet<String>();
            for (int i = 1; i < count; i++) {
                if (tags[i] == 7) {
                    addInternalName(references, (String) pool[(Integer) pool[i]]);
                } else if (tags[i] == 1) {
                    Matcher matcher = DESCRIPTOR_TYPE.matcher((String) pool[i]);
                    while (matcher.find()) {
                        addInternalName(references, matcher.group(1));
                    }
                }
            }
            references.remove(internalName.replace('/', '.'));
            return new ClassFileInfo(internalName.replace('/', '.'), major, references);
        } catch (IndexOutOfBoundsException | ClassCastException e) {
            throw new IOException("malformed class constant pool", e);
        }
    }

    private static void addInternalName(Set<String> names, String internalName) {
        if (internalName == null || internalName.length() == 0 || internalName.charAt(0) == '[') {
            return;
        }
        names.add(internalName.replace('/', '.'));
    }

    private static void skip(DataInputStream input, int length) throws IOException {
        int remaining = length;
        while (remaining > 0) {
            int skipped = input.skipBytes(remaining);
            if (skipped == 0) {
                throw new IOException("truncated class file");
            }
            remaining -= skipped;
        }
    }
}
