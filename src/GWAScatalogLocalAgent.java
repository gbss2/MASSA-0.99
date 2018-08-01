import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

public class GWAScatalogLocalAgent extends DBagent{
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
	public GWAScatalogLocalAgent() {
    	this.setDBname("gwas");
   	 	this.setInformation("gwas");
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

		public waitRequest() {
			 MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
		}

		public void action() {
			ACLMessage msg = myAgent.receive(simplerequest_template);
			if (msg != null) {
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					sender = msg.getSender();
					snpdata = (BioDataLite)msg.getContentObject();
					addBehaviour(new GWASAction(snpdata.getSnpIdList(),sender,snpdata.getSearchid()));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class GWASAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public AID partneragent;
		public int[] snpidlist;

		public GWASAction(int[] sl, AID pa, int asi){
			partneragent = pa;
			snpidlist = sl;
			setAnnsearchid(asi);

			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessGWAS(snpidlist), STATE_A);
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
	class accessGWAS extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private int[] snpidlist;

		public accessGWAS (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;
		}

		public void onStart(){
			for (int i = 0; i < snpidlist.length; i++) {
				if(snpidlist[i]>0){
					this.addSubBehaviour(new mysqlLocalGWASquery(snpidlist[i]));
				}
			}
		}

		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished task.");
			return super.onEnd();
		}
	}

	/* Agent action: Query DB  */	
	class mysqlLocalGWASquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private int snpid;

		public mysqlLocalGWASquery(int l) { snpid = l; }
		public void action() { QueryGWAS(snpid); }
	}

	/* LIST OF QUERIES */
	
	/* Agent action: Query SNP  */
	public void QueryGWAS(int s) {
		String snpid = "rs"+Integer.toString(s);
		
		List<String> pubmedid = new ArrayList<String>();
		List<String> reportedgenes = new ArrayList<String>();
		List<String> mappedgene = new ArrayList<String>();
		List<String> riskallele = new ArrayList<String>();
		List<String> context = new ArrayList<String>();
		List<String> pvalue = new ArrayList<String>();
		List<String> pvaluetext = new ArrayList<String>();
		List<String> confidenceinterval = new ArrayList<String>();
		List<String> disease_trait = new ArrayList<String>();
		List<String> mapped_trait = new ArrayList<String>();
		List<String> initial_sample_size = new ArrayList<String>();
		
		String pid= "null";
		String rg = "null";
		String mg = "null";
		String ra = "null";
		String ct = "null";
		String pv = "null";
		String pvt = "null"; 
		String or = "null";
		String ci = "null";
		String dt = "null";
		String mt = "null";
		String ss = "null";


		String strsql = "SELECT pubmedid, " +
						"reported_genes, " +
						"mapped_gene, " +
						"disease_trait, " +
						"initial_sample_size, " +
						"strongest_snp_risk_allele, " +
						"context, " +
						"pvalue, " +
						"pvalue_text, " +
						"OR_or_beta, " +
						"95_CI, " +
						"mapped_trait " +
						"FROM gwas_04_2018 " +
						"WHERE " +
						"snps = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,snpid);
			ResultSet rs = pstmt.executeQuery();	
	    
			while (rs.next()) {
				if(!pubmedid.contains(rs.getString("pubmedid"))){
					pubmedid.add(rs.getString("pubmedid"));
				}
	
				if(!reportedgenes.contains(rs.getString("reported_genes"))){
					reportedgenes.add(rs.getString("reported_genes"));
				}
	
				if(!riskallele.contains(rs.getString("strongest_snp_risk_allele"))){
					riskallele.add(rs.getString("strongest_snp_risk_allele"));
				}
	
				if(!context.contains(rs.getString("context"))){
					context.add(rs.getString("context"));
				}
	
				if(!pvalue.contains(rs.getString("pvalue"))){
					pvalue.add(rs.getString("pvalue"));
				}
				
				if(!disease_trait.contains(rs.getString("disease_trait"))){
					disease_trait.add(rs.getString("disease_trait"));
				}
				
				if(!initial_sample_size.contains(rs.getString("initial_sample_size"))){
					initial_sample_size.add(rs.getString("initial_sample_size"));
				}
			}

			rs.close();
			pstmt.close();
	
			Iterator<String> it;
			it = pubmedid.iterator();
			while(it.hasNext()) {
			    String item = (String)it.next();
			    if(!pid.equals("null")){ pid = pid + item +";"; }
			    else{ pid = item +";"; }
			}
	
			it = reportedgenes.iterator();
			while(it.hasNext()) {
			    String item = (String)it.next();
			    if(!rg.equals("null")){ rg = rg + item +";"; }
			    else{ rg = item +";"; }
			}
	
			it = riskallele.iterator();
			while(it.hasNext()) {
			    String item = (String)it.next();
			    if(!ra.equals("null")){ ra = ra + item +";"; }
			    else{ ra = item +";"; }
			}
	
			it = context.iterator();
			while(it.hasNext()) {
			    String item = (String)it.next();
			    if(!ct.equals("null")){ ct = ct + item +";"; }
			    else{ ct = item +";"; }
			}
	
			it = pvalue.iterator();
			while(it.hasNext()) {
			    String item = (String)it.next();
			    if(!pv.equals("null")){ pv = pv + item +";"; }
			    else{ pv = item +";"; }
			}
	
			it = disease_trait.iterator();
			while(it.hasNext()) {
			    String item = (String)it.next();
			    if(!dt.equals("null")){ dt = dt + item +";"; }
			    else{ dt = item +";"; }
			}
	
			it = initial_sample_size.iterator();
			while(it.hasNext()) {
			    String item = (String)it.next();
			    if(!ss.equals("null")){ ss = ss + item +";"; }
			    else{ ss = item +";"; }
			}
	
			String sql = "insert ignore into gwascatalog (snps,pubmedid,disease_trait,initial_sample_size,reported_genes,strongest_snp_risk_allele,context,pvalue,fk_searchid) values (?,?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
			pstmt2.setString(1,snpid);
			pstmt2.setString(2,pid);
			pstmt2.setString(3,dt);
			pstmt2.setString(4,ss);
			pstmt2.setString(5,rg);
			pstmt2.setString(6,ra);
			pstmt2.setString(7,ct);
			pstmt2.setString(8,pv);
			pstmt2.setInt(9,this.getAnnsearchid());
			
			pstmt2.executeUpdate();
			pstmt2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}