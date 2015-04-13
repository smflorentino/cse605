package java.lang;

import java.io.File;
import java.io.IOException;

final class FCProcess {
    static native Process exec(String[] cmd, String[] env, File dir) throws IOException;

}
