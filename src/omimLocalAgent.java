import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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

public class omimLocalAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	BioDataLite omimdata;
	int input_length;
	int[] input;
	public String sn = "localhost";
	public String md = "massa";
	public String user = "massa";
	public String key = "@sdfghjkl!";

	/* Constructor */
	public omimLocalAgent() {
		this.setDBname("omim");
		this.setInformation("omim");
		this.omimdata = new BioDataLite();
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
				sender = msg.getSender();
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					BioDataLite contentdata = (BioDataLite)msg.getContentObject();
					setAnnsearchid(contentdata.getSearchid());
					omimdata.setSearchid(getAnnsearchid());
					addBehaviour(new omimAction(sender,contentdata));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class omimAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";

		public omimAction(AID pa, BioDataLite bdlite){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessOMIM(bdlite), STATE_A);
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
	class accessOMIM extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		BioDataLite contentdata;

		public accessOMIM (BioDataLite bdl) {
			super(WHEN_ALL);
			contentdata = bdl;
		}

		public void onStart(){
			String query = "SELECT * FROM gene WHERE fk_searchid="+getAnnsearchid();

			try {
				Statement st = annconnection.createStatement();
				ResultSet rs = st.executeQuery(query);
		
			    while (rs.next()) {
			    	this.addSubBehaviour(new mysqlLocalOMIMquery(rs.getString("gene_symbol")));
			    }
		
				st.close();
				rs.close();
			} catch (SQLException e){ e.printStackTrace(); }
		}
			
		public int onEnd(){
				System.out.println("Agent " + getLocalName() +" finished.");
				return super.onEnd();
		}
	}

	/* Agent action: Query DB  */	
	class mysqlLocalOMIMquery extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private String genesymbol;
			
		public mysqlLocalOMIMquery(String gs) { genesymbol = gs; }
		public void action() { QueryGeneMap2(genesymbol); }
	}



	/* LIST OF QUERIES */
	
	/* Agent action: Query OMIM GENEMAP2  */
	public void QueryGeneMap2(String genesymbol) {
		String cl = "";
		String ds = "";
		String cm = "";
		String rf = "";
		String gs = "";
		String mt = "";
		
		List<String> dsl;
		List<String> cml;
		List<String> cll;
		List<String> gsl;
		List<String> mtl;

		dsl = new ArrayList<String>();
		cml = new ArrayList<String>();
		cll = new ArrayList<String>();
		gsl = new ArrayList<String>();
		mtl = new ArrayList<String>();
			
		String strsql = "select genemap2_04_2018.approvedSymbol, " +
						"genemap2_04_2018.cytoLocation, " +
						"genemap2_04_2018.phenotypes, " +
						"genemap2_04_2018.mimNumber, " +
						"genemap2_04_2018.comments, " +
						"genemap_04_2018.mappingMethod " +
						"FROM genemap2_04_2018 " +
						"LEFT JOIN genemap_04_2018 ON genemap2_04_2018.mimNumber = genemap_04_2018.mimNumber " +
						"WHERE " +
						"genemap2_04_2018.approvedSymbol = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			ResultSet rs = pstmt.executeQuery();	

			while (rs.next()) {
				String cytoLoc = rs.getString("cytoLocation");
				String geneStatus = "null";
				String method = rs.getString("mappingMethod");
				String disorder1 = rs.getString("phenotypes");
			//	String disorder2 = rs.getString("disorder2");
			//	String disorder3 = rs.getString("disorder3");
				String comments1 = rs.getString("comments");
			//	String comments2 = rs.getString("comments2");
				String reference = rs.getString("mimNumber");
				
				if(!cll.contains(cytoLoc)){
					cl = cl+";"+cytoLoc;
					cll.add(cytoLoc);
				}
				
				if(!gsl.contains(geneStatus) && geneStatus != "null"){
					gs = gs+";"+geneStatus;
					gsl.add(geneStatus);
				}
				
				if(!mtl.contains(method) && method != "null"){
					mt = mt+";"+method;
					mtl.add(method);
				}
				
				if(!dsl.contains(disorder1) && disorder1 != "null"){
					ds = ds+";"+disorder1;
					dsl.add(disorder1);
				}

//				if(!dsl.contains(disorder2) && disorder2 != "null"){
//					ds = ds+";"+disorder2;
//					dsl.add(disorder2);
//				}

//				if(!dsl.contains(disorder3)  && disorder3 != "null"){
//					ds = ds+";"+disorder3;
//					dsl.add(disorder3);
//				}

				if(!cml.contains(comments1)  && comments1 != "null"){
					cm = cm+";"+comments1;
					cml.add(comments1);
				}

//				if(!cml.contains(comments2) && comments2 != "null"){
//					cm = cm+";"+comments2;
//					cml.add(comments2);
//				}
				
				rf = rf+";"+reference;
			}
			
			rs.close();
			pstmt.close();

			if(ds.length()>0){ ds = ds.substring(1); }
			else { ds = "null"; }

			if(cm.length()>0){ cm = cm.substring(1); }
			else { cm = "null"; }
			
			if(cl.length()>0){ cl = cl.substring(1); }
			else { cl = "null"; }

			if(gs.length()>0){ gs = gs.substring(1); }
			else { gs = "null"; }

			if(mt.length()>0){ mt = mt.substring(1); }
			else { mt = "null"; }

			if(rf.length()>0){ rf = rf.substring(1); }
			else { rf = "null"; }

			String sql = "insert ignore into omim (fk_searchid,cytoLoc,geneStatus,methods,disorder,comments,reference,gene_symbol) values (?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
			pstmt2.setInt(1,this.getAnnsearchid());
			pstmt2.setString(2,cl);
			pstmt2.setString(3,gs);
			pstmt2.setString(4,mt);
			pstmt2.setString(5,ds);
			pstmt2.setString(6,cm);
			pstmt2.setString(7,rf);
			pstmt2.setString(8,genesymbol);
				
			pstmt2.executeUpdate();
			pstmt2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}

	
	/* Agent action: Query OMIM 1  */
	public void QueryOMIM(String genesymbol) {
		String cl = "";
		String ds = "";
		String cm = "";
		String rf = "";
		String gs = "";
		String mt = "";
		
		List<String> dsl;
		List<String> cml;
		List<String> cll;
		List<String> gsl;
		List<String> mtl;

		dsl = new ArrayList<String>();
		cml = new ArrayList<String>();
		cll = new ArrayList<String>();
		gsl = new ArrayList<String>();
		mtl = new ArrayList<String>();
			
		String strsql = "select genemap.geneSymbol," +
						"genemap.cytoLoc, " +
						"genemap.geneStatus, " +
						"genemap.method, " +
						"genemap.disorder1, " +
						"genemap.disorder2, " +
						"genemap.disorder3, " +
						"genemap.mimId, " +
						"genemap.comments1, " +
						"genemap.comments2, " +
						"genemap.reference " +
						"FROM genemap " +
						"WHERE " +
						"genemap.geneSymbol = ? ;";

		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			ResultSet rs = pstmt.executeQuery();	

			while (rs.next()) {
				String cytoLoc = rs.getString("cytoLoc");
				String geneStatus = rs.getString("geneStatus");
				String method = rs.getString("method");
				String disorder1 = rs.getString("disorder1");
				String disorder2 = rs.getString("disorder2");
				String disorder3 = rs.getString("disorder3");
				String comments1 = rs.getString("comments1");
				String comments2 = rs.getString("comments2");
				String reference = rs.getString("reference");
				
				if(!cll.contains(cytoLoc)){
					cl = cl+";"+cytoLoc;
					cll.add(cytoLoc);
				}
				
				if(!gsl.contains(geneStatus) && geneStatus != "null"){
					gs = gs+";"+geneStatus;
					gsl.add(geneStatus);
				}
				
				if(!mtl.contains(method) && method != "null"){
					mt = mt+";"+method;
					mtl.add(method);
				}
				
				if(!dsl.contains(disorder1) && disorder1 != "null"){
					ds = ds+";"+disorder1;
					dsl.add(disorder1);
				}

				if(!dsl.contains(disorder2) && disorder2 != "null"){
					ds = ds+";"+disorder2;
					dsl.add(disorder2);
				}

				if(!dsl.contains(disorder3)  && disorder3 != "null"){
					ds = ds+";"+disorder3;
					dsl.add(disorder3);
				}

				if(!cml.contains(comments1)  && comments1 != "null"){
					cm = cm+";"+comments1;
					cml.add(comments1);
				}

				if(!cml.contains(comments2) && comments2 != "null"){
					cm = cm+";"+comments2;
					cml.add(comments2);
				}
				
				rf = rf+";"+reference;
			}
			
			rs.close();
			pstmt.close();

			if(ds.length()>0){ ds = ds.substring(1); }
			else { ds = "null"; }

			if(cm.length()>0){ cm = cm.substring(1); }
			else { cm = "null"; }
			
			if(cl.length()>0){ cl = cl.substring(1); }
			else { cl = "null"; }

			if(gs.length()>0){ gs = gs.substring(1); }
			else { gs = "null"; }

			if(mt.length()>0){ mt = mt.substring(1); }
			else { mt = "null"; }

			if(rf.length()>0){ rf = rf.substring(1); }
			else { rf = "null"; }

			String sql = "insert ignore into omim (fk_searchid,cytoLoc,geneStatus,methods,disorder,comments,reference,gene_symbol) values (?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
			pstmt2.setInt(1,this.getAnnsearchid());
			pstmt2.setString(2,cl);
			pstmt2.setString(3,gs);
			pstmt2.setString(4,mt);
			pstmt2.setString(5,ds);
			pstmt2.setString(6,cm);
			pstmt2.setString(7,rf);
			pstmt2.setString(8,genesymbol);
				
			pstmt2.executeUpdate();
			pstmt2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
	
	/* Agent action: Query OMIM MORBIDMAP  */	
	public void QueryOMIMmorbidmap(String genesymbol) {
		String cl = "";
		String ds = "";
		String pm = "";
		String mimId = "";
			
		List<String> dsl;
		List<String> pml;
		List<String> cll;
		List<String> mid;

		dsl = new ArrayList<String>();
		pml = new ArrayList<String>();
		cll = new ArrayList<String>();
		mid = new ArrayList<String>();
		
		String strsql = "select morbidmap.gene," +
						"morbidmap.cytoLoc, " +
						"morbidmap.phenomap, " +
						"morbidmap.disorder, " +
						"morbidmap.omimDisorderId " +
						"FROM morbidmap " +
						"WHERE " +
						"morbidmap.gene = ? ;";

		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,genesymbol);
			ResultSet rs = pstmt.executeQuery();	

			while (rs.next()) {
				String cytoLoc = rs.getString("cytoLoc");
				String disorder = rs.getString("disorder");
				String omimId = rs.getString("omimDisorderId");
				String phenoMap = rs.getString("phenoMap");
				
				if(!cll.contains(cytoLoc)){
					cl = cl+";"+cytoLoc;
					cll.add(cytoLoc);
				}
				
				if(!dsl.contains(disorder) && disorder != "null"){
					ds = ds+";"+disorder;
					dsl.add(disorder);
					
					pm = pm+";"+phenoMap;
					pml.add(phenoMap);
					
					mimId = mimId+";"+omimId;
					mid.add(omimId);
				}
			}
			
			rs.close();
			pstmt.close();

			if(ds.length()>0){ ds = ds.substring(1); }
			else { ds = "null"; }

			if(pm.length()>0){ pm = pm.substring(1); }
			else { pm = "null"; }

			if(cl.length()>0){ cl = cl.substring(1); }
			else { cl = "null"; }

			if(mimId.length()>0){ mimId = mimId.substring(1); }
			else { mimId = "null"; }

			String sql = "insert ignore into omim (fk_searchid,cytoLoc,phenMap,disorder,mimId,gene_symbol) values (?,?,?,?,?,?)";
			PreparedStatement pstmt2 = annconnection.prepareStatement(sql);
			pstmt2.setInt(1,this.getAnnsearchid());
			pstmt2.setString(2,cl);
			pstmt2.setString(3,pm);
			pstmt2.setString(4,ds);
			pstmt2.setString(5,mimId);
			pstmt2.setString(6,genesymbol);
				
			pstmt2.executeUpdate();
			pstmt2.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}
}