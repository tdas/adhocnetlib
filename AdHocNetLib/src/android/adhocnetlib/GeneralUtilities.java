package android.adhocnetlib;

import android.util.Log;

public class GeneralUtilities {
	
	/*static {
        try {
            Logd("Trying to load libnativetask.so");
            System.loadLibrary("nativetask");
        }
        catch (UnsatisfiedLinkError ule) {
            Loge("Could not load libnativetask.so");
        }
    }
	*/
	public static ShellCommand shell = new ShellCommand();
	
	
	public static boolean runRootCommand(String command) {
		Logd("Root-Command ==> \""+command+"\"");
		/*int returncode = runCommand("su -c \""+command+"\"");
    	if (returncode == 0) {
			return true;
		}
    	Logd( "Root-Command error, return code: " + returncode);
		*/
		ShellCommand.CommandResult r = shell.su.runWaitFor(command);		 
		if (!r.success()) {
		     Logd("Error " + r.stderr);
		     return false;
		} else {
		     Logd("Successfully executed getprop wifi.interface. Result: " + r.stdout);		     
		     return true;
		}
    }
	
	private static void Logd(String msg) {
		Log.d("GeneralUtilities", msg);
	}
	
	private static void Loge(String msg) {
		Log.e("GeneralUtilities", msg);
	}
}
