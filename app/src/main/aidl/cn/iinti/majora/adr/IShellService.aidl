// IShellService.aidl
package cn.iinti.majora.adr;
import java.util.List;

// Declare any non-default types here with import statements

interface IShellService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    List<String> run(String cmd);
}