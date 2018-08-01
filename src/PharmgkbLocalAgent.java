import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.IOException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;


public class PharmgkbLocalAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	public String sn = "localhost";
	public String md = "massa";
	public String pharmuser = "massa";
	public String pharmkey = "@sdfghjkl!";
	BioDataLite snpdata;

	/* Constructor */
	public PharmgkbLocalAgent() {
		this.setDBname("dbpharmgkb");
		this.setInformation("pharmgkb");
//		this.pharmdata = new BioDataLite();
	}

	/* Agent setup */
	protected void setup() {
		System.out.println("Agent "+getLocalName()+" started.");
		this.register();
		this.dbConnect(sn, md, pharmuser, pharmkey);
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
				sender = msg.getSender();
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					snpdata = (BioDataLite)msg.getContentObject();
					addBehaviour(new PharmAction(snpdata.getSnpIdList(), sender, snpdata.getSearchid()));
//					setAnnsearchid(contentdata.getSearchid());
//					pharmdata.setSearchid(getAnnsearchid());
//					addBehaviour(new PharmAction(sender,contentdata));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class PharmAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";

		public PharmAction(int[] sl, AID pa,int asi){

			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessPharm(sl), STATE_A);
			this.registerLastState(new SendReply(pa), STATE_B);
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
			msgcontent = new BioDataLite();
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
	class accessPharm extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

//		BioDataLite contentdata;
		private int[] snpidlist;

		public accessPharm (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;
		}

		public void onStart(){
			
			for (int i = 0; i < snpidlist.length; i++) {
				if(snpidlist[i]>0){ 
					this.addSubBehaviour(new mysqlLocalPharmquery(snpidlist[i])); 
				}
			}
		}			

		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}

	/* Agent action: Query DB  */	
	class mysqlLocalPharmquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private int snpid;
//		private boolean errflag;

		public mysqlLocalPharmquery(int l) { 
			this.snpid = l; 
//			this.errflag = false;
		}
		public void action() { QueryPGKBClinicalVariants(snpid); }
	}


	/* LIST OF QUERIES */
	
	/* Agent action: Query clinical_variants  */	
	public void QueryPGKBClinicalVariants(int s) {
		String snpid = "rs"+Integer.toString(s);
		PreparedStatement pstmt;
		String strsql;
		ResultSet rs;

		String chemical = "";
		String lvlEv = "";
		String pharmPheno = "";
		String phenotype = "";
		
		strsql = "select chemicals, " +
				 "levelOfEvidence, " +
				 "types, " +
				 "phenotypes " +
				 "FROM clinical_variants " +
				 "WHERE " +
				 "variant = ? ;";

		try {
			pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,snpid);
			rs = pstmt.executeQuery();	

			while (rs.next()) {
				chemical = chemical +";"+ rs.getString(1);
				lvlEv = lvlEv +";"+ rs.getString(2);
				pharmPheno = pharmPheno +";"+ rs.getString(3);
				phenotype = phenotype +";"+ rs.getString(3);
			}
			
			rs.close();
			pstmt.close();

			if(chemical.length()>0){ chemical = chemical.substring(1); }
			else { chemical = "null"; }

			if(lvlEv.length()>0){ lvlEv = lvlEv.substring(1); }
			else { lvlEv = "null"; }

			if(pharmPheno.length()>0){ pharmPheno = pharmPheno.substring(1); }
			else { pharmPheno = "null"; }
			
			if(phenotype.length()>0){ phenotype = phenotype.substring(1); }
			else { phenotype = "null"; }
			
			String sql = "insert ignore into pharmGKB_snp (snpid,chemicals,levelOfEvidence,types,phenotypes,fk_searchid) values (?,?,?,?,?,?)";
			PreparedStatement pstm2 = annconnection.prepareStatement(sql);
			pstm2.setString(1,snpid);
			pstm2.setString(2,chemical);
			pstm2.setString(3,lvlEv);
			pstm2.setString(4,pharmPheno);
			pstm2.setString(5,phenotype);
			pstm2.setInt(6,getAnnsearchid());
			
			pstm2.executeUpdate();
			pstm2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
	
	/* Agent action: Query PGKB DB - DEPRECATED  */	
	public void QueryPGKBLocalSimplified(String genesymbol) {
		PreparedStatement pstmt;
		String strsql;
		ResultSet rs;

		String pw = "";
		String ds = "";
		String dg = "";
		
		strsql = "select distinct gene.pgkbkey \"geneSymbol\" ," +
				 "goldenPath.name \"polyLoc\", " +
				 "goldenPath.pgkbKey \"polyId\", " +
				 "drug.name \"drug\", " +
				 "disease.name \"disease\", " +
				 "pathway.type \"pathName\" " +
				 "FROM gene " +
				 "LEFT JOIN goldenPath ON gene.fk = goldenPath.fk " +
				 "LEFT JOIN drug ON gene.fk = drug.fk " +
				 "LEFT JOIN disease ON gene.fk = disease.fk " +
				 "LEFT JOIN pathway ON gene.fk = pathway.fk " +
				 "WHERE " +
				 "gene.pgkbkey = ? " +
				 "GROUP BY polyLoc, polyId, drug, disease, pathName " +
				 "HAVING COUNT(*) = 1;";

		try {
			pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			rs = pstmt.executeQuery();	

			while (rs.next()) {
				dg = dg +";"+ rs.getString("drug");
				ds = ds +";"+ rs.getString("disease");
				pw = pw +";"+ rs.getString("pathName");
			}
			
			rs.close();
			pstmt.close();

			if(dg.length()>0){ dg = dg.substring(1); }
			else { dg = "null"; }

			if(pw.length()>0){ pw = pw.substring(1); }
			else { pw = "null"; }

			if(ds.length()>0){ ds = ds.substring(1); }
			else { ds = "null"; }
			
			String sql = "insert ignore into pharmGKB (geneSymbol,drugs,pathway,disease,fk_searchid) values (?,?,?,?,?)";
			PreparedStatement pstm2 = annconnection.prepareStatement(sql);
			pstm2.setString(1,genesymbol);
			pstm2.setString(2,dg);
			pstm2.setString(3,pw);
			pstm2.setString(4,ds);
			pstm2.setInt(5,this.getAnnsearchid());
			
			pstm2.executeUpdate();
			pstm2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}

	/* Agent action: Query PGKB DB - DEPRECATED  */	
	public void QueryPGKBLocal(String genesymbol) {
		String strsql;
		ResultSet rs;

		String pw ="";
		String ds = "";
		String dg = "";

		try {
			strsql = "select distinct drug.name \"drug\" FROM gene LEFT JOIN drug ON gene.fk = drug.fk WHERE gene.pgkbkey = ? ;";

			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			rs = pstmt.executeQuery();	

			while (rs.next()) { dg = dg +";"+ rs.getString("drug"); }

			strsql = "select distinct disease.name \"disease\" FROM gene LEFT JOIN disease ON gene.fk = disease.fk WHERE gene.pgkbkey = ? ;";

			pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			rs = pstmt.executeQuery();	

			while (rs.next()) { ds = ds +";"+ rs.getString("disease"); }

			strsql = "select distinct pathway.name \"pathName\", pathway.type \"pathType\" FROM gene LEFT JOIN pathway ON gene.fk = pathway.fk WHERE gene.pgkbkey = ? ;";

			pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			rs = pstmt.executeQuery();	

			while (rs.next()) { pw = pw +";"+ rs.getString("pathName"); }
			
			rs.close();
			pstmt.close();

			if(dg.length()>0){ dg = dg.substring(1); }
			else { dg = "null"; }

			if(pw.length()>0){ pw = pw.substring(1); }
			else { pw = "null"; }

			if(ds.length()>0){ ds = ds.substring(1); }
			else { ds = "null"; }
			
			String sql = "insert ignore into pharmGKB (geneSymbol,drugs,pathway,disease,fk_searchid) values (?,?,?,?,?)";
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
			pstmt2.setString(1,genesymbol);
			pstmt2.setString(2,dg);
			pstmt2.setString(3,pw);
			pstmt2.setString(4,ds);
			pstmt2.setInt(5,this.getAnnsearchid());
			
			pstmt2.executeUpdate();
			pstmt2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}