package Sleve;

import java.io.DataInputStream;
import java.io.IOException;

public class Monitor extends Thread{
	public static boolean alive=true;
	static DataInputStream tin=null;				//�����
	public Monitor() {
		Spy.rc("�����������˿�");
		alive=true;
	}
	@Override
	public void run() {
		Spy.rc("�������˿��߳̿���");
		try {
			tin=new DataInputStream(Spy.sc.getInputStream());
		} catch (IOException e) {}
		while(alive) {
			String msg=null;
			try {
				msg=tin.readUTF();
			} catch (IOException e) {alive=false;break;}
			if(msg.startsWith("<#CANCEL#>")) {
				Spy.rc("�յ�ȡ����Ϣ");
				Spy.canceled=true;
				close();
			}
		}
	}
	public static void close() {
		if(Spy.sc!=null&&!Spy.sc.isClosed()) {
			try {
				Spy.sc.close();
			} catch (IOException e) {}
		}
		if(Spy.tout!=null) {
			try {
				Spy.tout.close();
			} catch (IOException e) {}
		}
	}
}
