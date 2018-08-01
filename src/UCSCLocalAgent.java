import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

public class UCSCLocalAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	BioDataLite snpdata;;
	int input_length;
	int[] input;
	public String sn = "localhost";
	public String md = "massa";
	public String user = "massa";
	public String key = "@sdfghjkl!";
	MySQLcon myconnection;

	/* Constructor */
	public UCSCLocalAgent() {
		this.setDBname("ucsc");
		this.setInformation("ucsc");
	}

	/* Agent setup */
	protected void setup() {
		System.out.println("Agent "+getLocalName()+" started.");
		this.register();
		this.dbConnect(sn, md, user, key);
		addBehaviour(new waitRequest());
	}
	
	/* Agent shutdown */
	protected void takeDown() {
		System.out.println("Agent" + getLocalName() + " shutdown.");
		mysqlDisconnect(conn);
	}
	
	/* BEHAVIOURS */
	
	/* Agent communication: wait for messages */
	class waitRequest extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		private MessageTemplate simplerequest_template;
		private AID sender;

		public waitRequest() { MessageTemplate.MatchPerformative(ACLMessage.REQUEST); }

		public void action() {
			ACLMessage msg = myAgent.receive(simplerequest_template);
			if (msg != null) {
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					sender = msg.getSender();
					snpdata = (BioDataLite)msg.getContentObject();
					addBehaviour(new UCSCAction(snpdata.getSnpIdList(),sender,snpdata.getSearchid()));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class UCSCAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public AID partneragent;
		public int[] snpidlist;

		public UCSCAction(int[] sl, AID pa, int asi){
			partneragent = pa;
			snpidlist = sl;
			setAnnsearchid(asi);

			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessUCSC(snpidlist), STATE_A);
			this.registerLastState(new SendReply(partneragent), STATE_B);
			this.registerDefaultTransition(STATE_A,STATE_B);
		}

		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished task.");
			return super.onEnd();
		}
	}

	/* Agent reply: send message to Coordinator on task end */
	class SendReply extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private AID     msgreceiver;
		private String  msgperformative;
		private BioDataLite msgcontent;
		private ACLMessage msg;

		public SendReply(AID p) {
			msgreceiver     = p;
			msgcontent      = new BioDataLite();
			msgperformative = "INFORM";
			msgcontent.setSearchid(getAnnsearchid());
		}
		
		public void action() {
			try {
				System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(msgreceiver);
				msg.setLanguage("English");
				msg.setContentObject(msgcontent);
				myAgent.send(msg);
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/* Agent action: parallel access to DB -> remoteAccessDBsnpLocal  */
	class accessUCSC extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private int[] snpidlist;

		public accessUCSC (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;
		}

		public void onStart(){
			for (int i = 0; i < snpidlist.length; i++) {
				if(snpidlist[i]>0){ this.addSubBehaviour(new mysqlLocalUCSCquery(snpidlist[i])); }
			}
		}

		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}
	
	/* Agent action: Query DB  */
	class mysqlLocalUCSCquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private int snpid;

		public mysqlLocalUCSCquery(int l) { snpid = l; }
		public void action() { QueryUCSC1(snpid); }
	}


	/* LIST OF QUERIES */
	
	/* Agent action: Query SNP  */
	public void QueryUCSC1(int s) {
		String snpid = "rs"+Integer.toString(s);
		
		String strand = "null";
		String refUCSC = "null";
		String obsGen = "null";
		String ucscClass = "null";
		String ucscFunc = "null";

		String strsql = "SELECT snp150.strand, " +
						"snp150.refUCSC, " +
						"snp150.observed, " +
						"snp150.class, " +
						"snp150.func, " +
						"snp150CodingDbSnp.codons, " +
						"snp150CodingDbSnp.peptides " +
						"FROM snp150 " +
						"LEFT JOIN snp150CodingDbSnp ON snp150.name = snp150CodingDbSnp.name " +
						"WHERE " +
						"snp150.name = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,snpid);
			ResultSet rs = pstmt.executeQuery();	
		    
			while (rs.next()) {
				strand = rs.getString("strand");
				refUCSC = rs.getString("refUCSC");
				obsGen = rs.getString("observed");
				ucscClass = rs.getString("class");
				ucscFunc = rs.getString("func");
			}
			
			rs.close();
			pstmt.close();
			
			String sql = "insert ignore into ucsc (polID,strand,refUCSC,observed,class,func,fk_searchid) values ('"+snpid+"','"+strand+"','"+refUCSC+"','"+obsGen+"','"+ucscClass+"','"+ucscFunc+"',"+this.getAnnsearchid() +")";
			
			Statement stm = annconnection.createStatement();
			stm.executeUpdate(sql);
			stm.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}

	/* Agent action: Query Pathway  */
	public void QueryUCSCPathway(String g) {
		String cgapID = "";
		String path = "";
		String strsql = "SELECT cgapAlias.cgapID, " +
						"cgapBiocDesc.description " +
						"FROM cgapAlias " +
						"LEFT JOIN cgapBiocPathway ON cgapAlias.cgapID = cgapBiocPathway.cgapID " +
						"LEFT JOIN cgapBiocDesc ON cgapBiocPathway.mapID = cgapBiocDesc.mapID " +
						"WHERE " +
						"cgapAlias.alias = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,g);
			ResultSet rs = pstmt.executeQuery();	
		    
			while (rs.next()) {
				path = rs.getString(2);
				if(cgapID == ""){ cgapID = rs.getString(1); }
				else { cgapID=cgapID+";"+rs.getString(1); }
	
				if(path == ""){ path = rs.getString(2); }
				else { path=path+";"+rs.getString(2); }
			}
			
			rs.close();
			pstmt.close();
			
			String sql = "insert ignore into ucscGene (fk_searchid,cgapID,cgapPath) values (?,?,?)";
	
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
			pstmt2.setInt(1,this.getAnnsearchid());
			pstmt2.setString(2,cgapID);
			pstmt2.setString(3,path);
	
			Statement stm = annconnection.createStatement();
			
			stm.executeUpdate(sql);
			stm.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
	

}