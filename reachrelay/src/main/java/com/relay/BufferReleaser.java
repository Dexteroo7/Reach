//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.nio.Buffer;
//import java.nio.ByteBuffer;
//
///**
// * Direct buffers are garbage collected by using a phantom reference and a
// * reference queue. Every once a while, the JVM checks the reference queue and
// * cleans the direct buffers. However, as this doesn't happen
// * immediately after discarding all references to a direct buffer, it's
// * easy to OutOfMemoryError yourself using direct buffers. This function
// * explicitly calls the Cleaner method of a direct buffer.
// */
//public final class BufferReleaser {
//
//    private static final boolean directByteBufferCleanerCleanCallable;
//    private static final Method directByteBufferCleanerMethod;
//    private static final Method cleanerCleanMethod;
//    private static final Method viewedBufferMethod;
//
//    static {
//
//        boolean tmpDirectByteBufferCleanerCleanCallable;
//        Method tmpDirectByteBufferCleanerMethod;
//        Method tmpCleanerCleanMethod;
//        Method tmpViewedBufferMethod = null;
//        try {
//            final Class<?> directByteBufferClass = Class.forName("java.nio.DirectByteBuffer");
//            tmpDirectByteBufferCleanerMethod = directByteBufferClass.getDeclaredMethod("cleaner");
//            tmpDirectByteBufferCleanerMethod.setAccessible(true);
//            final Class<?> cleanerClass = Class.forName("sun.misc.Cleaner");
//            tmpCleanerCleanMethod = cleanerClass.getDeclaredMethod("clean");
//            tmpCleanerCleanMethod.setAccessible(true);
//            final Class<?> directBufferInterface = Class.forName("sun.nio.ch.DirectBuffer");
//            try {//Java 1.6
//                tmpViewedBufferMethod = directBufferInterface.getDeclaredMethod("viewedBuffer");
//            } catch (NoSuchMethodException nsme) {
//                try {//Java 1.7 and later
//                    System.out.println("Java6 error");
//                    tmpViewedBufferMethod = directBufferInterface.getDeclaredMethod("attachment");
//                    System.out.println("Java 7 OK");
//                } catch (NoSuchMethodException nsme2) {//it should never happen (but I haven't tested with AvianVM)
//                }
//            }
//            if (tmpViewedBufferMethod != null)
//                tmpViewedBufferMethod.setAccessible(true);
//            tmpDirectByteBufferCleanerCleanCallable = true;
//        } catch (Throwable t) {
//
//            tmpDirectByteBufferCleanerCleanCallable = false;
//            tmpDirectByteBufferCleanerMethod = null;
//            tmpCleanerCleanMethod = null;
//            tmpViewedBufferMethod = null;
//            System.out.println("Impossible to retrieve the required methods to release the native resources of direct NIO buffers");
//        }
//        directByteBufferCleanerCleanCallable = tmpDirectByteBufferCleanerCleanCallable;
//        directByteBufferCleanerMethod = tmpDirectByteBufferCleanerMethod;
//        cleanerCleanMethod = tmpCleanerCleanMethod;
//        viewedBufferMethod = tmpViewedBufferMethod;
//        if (directByteBufferCleanerCleanCallable)
//            System.out.println("the deallocator has been successfully initialized");
//    }
//
//    public void deleteBuffer(final Buffer realNioBuffer) {
//
//        if (directByteBufferCleanerCleanCallable) {//the mechanism is working, tries to use it
//            Object directByteBuffer = null;
//            if (realNioBuffer instanceof ByteBuffer) {//this buffer is a direct byte buffer
//                directByteBuffer = realNioBuffer;
//            } else {//this buffer is a view on a direct byte buffer, gets this viewed buffer
//                //first attempt (inspired of com.jme3.util.BufferUtils.destroyByteBuffer(Buffer) from JMonkeyEngine 3: http://www.jmonkeyengine.org)
//                if (viewedBufferMethod != null) {
//                    try {
//                        directByteBuffer = viewedBufferMethod.invoke(realNioBuffer);
//                    } catch (Throwable t) {
//                        System.out.println("Failed to get the viewed buffer");
//                    }
//                }
//                //last attempt, less straightforward, looks at each field
//                if (directByteBuffer == null) {
//
//                    for (Field field : realNioBuffer.getClass().getDeclaredFields()) {
//                        final boolean wasAccessible = field.isAccessible();
//                        if (!wasAccessible)
//                            field.setAccessible(true);
//                        try {
//                            final Object fieldValue = field.get(realNioBuffer);
//                            if (fieldValue != null && fieldValue instanceof ByteBuffer && ((ByteBuffer) fieldValue).isDirect()) {
//                                directByteBuffer = fieldValue;
//                                break;
//                            }
//                        } catch (Throwable t) {
//                            System.out.println("Failed to get the value of a byte buffer's field");
//                        } finally {
//                            if (!wasAccessible)
//                                field.setAccessible(false);
//                        }
//                    }
//                }
//            }
//            if (directByteBuffer != null) {
//                Object cleaner;
//                try {
//                    cleaner = directByteBufferCleanerMethod.invoke(directByteBuffer);
//                    if (cleaner != null) {
//                        cleanerCleanMethod.invoke(cleaner);
//                        System.out.println("Successfully cleaned");
//                    }
//                } catch (Throwable t) {
//                    System.out.println("Failed to use the cleaner of a byte buffer");
//                }
//            }
//        }
//    }
//}