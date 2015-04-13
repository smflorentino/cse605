package java.io;

final class FCFile {

    static native boolean canRead(String path);
    static native boolean exists(String path);
    static native boolean isDirectory(String path);
    static native boolean canWriteDirectory(File f);
    static native boolean canWrite(String path);
    static native boolean canExecute(String path);
    static native boolean create(String path) throws IOException;
    static native boolean delete(String path);
    static native String getAbsolutePath(String path);
    static native String toCanonicalForm(String path) throws IOException;
    static native String getName(String path);
    static native boolean isAbsolute(String path);
    static native boolean isFile(String path);
    static native boolean isHidden(String path);
    static native long lastModified(String path);
    static native long length(String path);
    static native String[] list(String path);
    static native boolean mkdir(String path);
    static native boolean setReadable(String path,boolean readable,boolean ownerOnly);
    static native boolean setWritable(String path,boolean readable,boolean ownerOnly);
    static native boolean setExecutable(String path,boolean readable,boolean ownerOnly);
    static native long getTotalSpace(String path);
    static native long getFreeSpace(String path);
    static native long getUsableSpace(String path);
    static native File[] listRoots();
    static native boolean renameTo(String from,String to);
    static native boolean setLastModified(String path,long time);
    static native boolean isCaseSensitive();

}
