import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBsnpLocalAgent extends DBagent {
	private static final long serialVersionUID = 1L;
	
	/* Attributes */
	BioDataLite snpdata;
	int input_length;
	int[] input;
	// Set host address (localhost)
	public String sn = "localhost";
	// Set database used
	public String md = "massa";
	// Set User Name
	public String user = "massa";
	// Set User Password
	public String key = "@sdfghjkl!";
	
	/* Constructor */
	public DBsnpLocalAgent() {
		super();
		this.setDBname("dbsnp");
   	 	this.setInformation("snp");
	}
	
	/* Agent setup */
	protected void setup() {
		System.out.println("Agent " + getLocalName() + " started.");
		/* Here DBsnpAgent must register */
		this.register();
		/* Here DBagent must connect with its local database */
   	 	this.dbConnect(sn, md, user, key);
   	 	/* Here DBagent must start cyclic behaviour */
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
					addBehaviour(new dbSNPAction(snpdata.getSnpIdList(),sender,snpdata.getSearchid()));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 
	
	/* Agent action: access DB -> accessDBsnpLocal; send reply  */
	class dbSNPAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public AID partneragent;
		public int[] snpidlist;
		
		public dbSNPAction(int[] sl, AID pa, int asi){
			partneragent = pa;
			snpidlist = sl;
			setAnnsearchid(asi);
			
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessDBsnpLocal(snpidlist), STATE_A);
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
	class accessDBsnpLocal extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private int[] snpidlist;
		
		public accessDBsnpLocal (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;

		}
		
		public void onStart(){
			for (int i = 0; i < snpidlist.length; i++) {
				if(snpidlist[i]>0){
					this.addSubBehaviour(new remoteAccessDBsnpLocal(snpidlist[i]));
				}
			}
		}
		
		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}
	
	/* Agent action: Query DB  */
	class remoteAccessDBsnpLocal extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private int snpid;

		public remoteAccessDBsnpLocal(int l) { snpid = l; }

		public void action() {
			clearPolTable();
			String mrnaaccid = QueryDBsnpLocal11(snpid);
			QueryDBsnpLocal12(snpid);
			QueryDBsnpLocal13(snpid);
			QueryDBsnpLocal14(snpid);
			QueryDBsnpLocal15(snpid,mrnaaccid);
			
			int key = 1;
			String sql = "insert into snp " +
			//		"(snppk," +						// 0
					"(snpid," +						// polTable[0] - 1
					"kind," +						// polTable[1] - 2
					"subkind," +					// polTable[2] - 3
					"referenceValue," +				// polTable[3] - 4
					"referenceAllele," +			// polTable[20] - 5
					"coordRelGene," +				// polTable[4] - 6
					"chromosome," +					// polTable[5] - 7
					"coordRefSeq," +				// polTable[6] - 8
					"assm_build_version," +			// polTable[7] - 9
					"assm_coord_start," +			// polTable[8] - 10
					"assm_coord_end," +				// polTable[9] - 11
					"value," +						// polTable[14] - 12
					"ancestral_allele," +			// polTable[15] - 13
					"orientation," +				// polTable[16] - 14
					"mrnaAcc," +					// polTable[17] - 15
					"mrnaVer," +					// polTable[18] - 16
					"freq," +						// polTable[19] - 17
					"fk_genepk," +					// polTable[] - 18
					"fk_searchid) " +				// polTable[] - 19
					"values " +
					"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			//		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			
			if(polTable[12] != null){
				if(!polTable[12].equals("NA") || !polTable[12].equals("null")){
					String genesql = "insert ignore into gene (gene_symbol,geneid,fk_searchid) values (?,?,?)";	
	
					try{
						PreparedStatement st = annconnection.prepareStatement(genesql,Statement.RETURN_GENERATED_KEYS);
						st.setString(1,polTable[13]); 
						st.setString(2,polTable[12]);
						st.setInt(3,getAnnsearchid());
						st.executeUpdate();	
	
						ResultSet rs = st.getGeneratedKeys();
						if(rs.next()) {
							key = rs.getInt(1);
						}
						st.close();
						rs.close();
					}catch(SQLException e) {
						System.out.println("SQL Exception... Error inserting value into gene annotation table.");
						e.printStackTrace();
					}
				}
			} else { key = 1; }

			try {
				PreparedStatement stm = annconnection.prepareStatement(sql);
				stm.setString(1,Integer.toString(snpid)); // snpid

				stm.setString(2,polTable[1]); // kind
				stm.setString(3,polTable[2]); // subkind
				stm.setString(4,polTable[3]); // referenceValue
				stm.setString(5,polTable[20]); // referenceAllele
				stm.setString(6,polTable[4]); // coordRelGene
				stm.setString(7,polTable[5]); // chromosome
				stm.setString(8,polTable[6]); // coordRefSeq
				stm.setString(9,polTable[7]); // assm_build_version
				stm.setString(10,polTable[8]); // assm_coord_start
				stm.setString(11,polTable[9]); // assm_coord_end
				stm.setString(12,polTable[14]); // value
				stm.setString(13,polTable[15]); // ancestral_allele
				stm.setString(14,polTable[16]); // orientation
				stm.setString(15,polTable[17]); // mrnaAcc
				stm.setString(16,polTable[18]); // mrnaVer
				stm.setString(17,polTable[19]); // freq
				stm.setInt(18,key); // fk_genepk
				stm.setInt(19,getAnnsearchid()); // fk_searchid
				stm.executeUpdate();
				stm.close();
			} catch(SQLException e) {
				System.out.println("SQL Exception... Error inserting value into snp annotation table.");
				e.printStackTrace();
			}
		}
	}


	/* Agent action: Query 1  */
	public String QueryDBsnpLocal11(int snpid) {
		String mRnaAcc = "";
		String strsql = "SELECT b150_SNPChrPosOnRef_105.snp_id, " +
		//		"b137_MapLinkHGVS.snp_type, " +
				"b150_SNPChrPosOnRef_105.chr, " +
				"b150_SNPChrPosOnRef_105.pos, " +
				"b150_SNPContigLocusId_105.build_id, " +
				"b150_SNPContigLocusId_105.asn_from, " +
				"b150_SNPContigLocusId_105.asn_to, " +
				"b150_SNPChrPosOnRef_105.orien, " +
				"b150_SNPContigLocusId_105.locus_id, " +
				"b150_SNPContigLocusId_105.locus_symbol, " +
				"b150_SNPContigLocusId_105.mrna_acc, " +
				"b150_SNPContigLocusId_105.mrna_ver " +
				"FROM b150_SNPChrPosOnRef_105 " +
				"LEFT JOIN b150_SNPContigLocusId_105 ON b150_SNPChrPosOnRef_105.snp_id = b150_SNPContigLocusId_105.snp_id " +
		//		"LEFT JOIN b137_SNPContigLocusId ON SNP.snp_id = b137_SNPContigLocusId.snp_id " +
		//		"LEFT JOIN b137_SNPChrPosOnRef ON SNP.snp_id = b137_SNPChrPosOnRef.snp_id " +
				"WHERE " +
				"b150_SNPChrPosOnRef_105.snp_id = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,Integer.toString(snpid));
			ResultSet rs = pstmt.executeQuery();	
		
			while (rs.next()) {
				String polCode = rs.getString("snp_id");
			//	String polType = rs.getString("snp_type");
				String polType = "null";
				String chr = rs.getString("chr");
				String coordRefSeq = rs.getString("pos");
				String assmBuildVer = rs.getString("build_id");
				String assmCoordStart = rs.getString("asn_from");
				String assmCoordEnd = rs.getString("asn_to");
				String geneId = rs.getString("locus_id");
				String geneName = rs.getString("locus_symbol");
				String orient = rs.getString("orien");
				mRnaAcc = rs.getString("mrna_acc");
				String mRnaVer = rs.getString("mrna_ver");
				
				polTable[0]= polCode;
				polTable[1]= polType;
				polTable[5]= chr;
	
				if(coordRefSeq == null){ polTable[6]= coordRefSeq; }
				else{ polTable[6]= Integer.toString((Integer.parseInt(coordRefSeq)+1)); }	
	
				polTable[7]= assmBuildVer;
	
				if(assmCoordStart == null){ polTable[8]= assmCoordStart; }
				else{ polTable[8]= assmCoordStart+1; }
	
				if(assmCoordEnd == null){ polTable[9]= assmCoordEnd; }
				else{ polTable[9]= assmCoordEnd+1; }
	
				polTable[12]= geneId;
				polTable[13]= geneName;
				polTable[16]= orient;
				polTable[17]= mRnaAcc;
				polTable[18]= mRnaVer;
			}
		
			rs.close();
			pstmt.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
		
		return mRnaAcc;
	}
	
	/* Agent action: Query 2  */
	public void QueryDBsnpLocal12(int snpid) {
		String strsql = "SELECT SNPAncestralAllele.snp_id, " +
				"Allele.allele " +
				"FROM Allele " +
				"LEFT JOIN SNPAncestralAllele ON SNPAncestralAllele.ancestral_allele_id = Allele.allele_id " +
				"WHERE " +
				"SNPAncestralAllele.snp_id = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,Integer.toString(snpid));
			ResultSet rs = pstmt.executeQuery();
			
			polTable[15]= "";	
			while (rs.next()) {
				String ancAllele = rs.getString("allele");
				if(polTable[15] == ""){ polTable[15] = ancAllele; } 
				else{ polTable[15]=polTable[15]+","+ancAllele; }
			}
		
			rs.close();
			pstmt.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}

	/* Agent action: Query 3  */
	public void QueryDBsnpLocal13(int snpid) {
/*		String strsql = "SELECT SNPAlleleFreq.snp_id, " +
				"SNPAlleleFreq.freq, " +
				"Allele.allele " +
				"FROM Allele " +
				"LEFT JOIN SNPAlleleFreq ON SNPAlleleFreq.allele_id = Allele.allele_id " +
				"WHERE " +
				"SNPAlleleFreq.snp_id = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,Integer.toString(snpid));
			ResultSet rs = pstmt.executeQuery();	

			polTable[19]="";
			polTable[20]="";
			while (rs.next()) {
				String refAllele = rs.getString("allele");
				String freq = rs.getString("freq");
				
				if(polTable[20] == ""){ polTable[20] = refAllele; }
				else{ polTable[20]=polTable[20]+","+refAllele; }
				
				if(polTable[19] == ""){ polTable[19] = freq; } 
				else{ polTable[19]=polTable[19]+","+freq; }
				
				polTable[3] = polTable[20];
			}
		
			rs.close();	
			pstmt.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
*/
		polTable[20] = "ref";
		polTable[19] = "freq";
		polTable[3] = "ref";
	}

	/* Agent action: Query 4  */
	public void QueryDBsnpLocal14(int snpid) {
		String strsql = "SELECT SnpFunctionCode.abbrev " +
				"FROM b150_SNPContigLocusId_105 " +
				"LEFT JOIN SnpFunctionCode ON b150_SNPContigLocusId_105.fxn_class = SnpFunctionCode.code" +
				" WHERE " +
				"b150_SNPContigLocusId_105.snp_id = ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,Integer.toString(snpid));
			ResultSet rs = pstmt.executeQuery();	
	    
			while (rs.next()) {
				String transcReg = rs.getString("abbrev");
				polTable[2]= transcReg;
			}
		
			rs.close();
			pstmt.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
	}

	/* Agent action: Query 5 */
	public void QueryDBsnpLocal15(int snpid, String mRNAaccId) {
/*		if(mRNAaccId == null){ mRNAaccId = ""; }
		
		String strsql = "SELECT SNP_HGVS.hgvs_name, " +
				"SNP_HGVS.source " +
				"FROM SNP_HGVS " +
				" WHERE " +
				"SNP_HGVS.snp_id = ? " +
				" AND SNP_HGVS.hgvs_name like ? ;";
		
		try {
			PreparedStatement pstmt = conn.prepareStatement(strsql);
			pstmt.setString(1,Integer.toString(snpid));
			pstmt.setString(2,mRNAaccId+"%");
			ResultSet rs = pstmt.executeQuery();	
		    
			while (rs.next()) {
				String coordRelGene = rs.getString("hgvs_name");
				String source = rs.getString("source");
				
				if(coordRelGene!=null){
					String aux = coordRelGene.split(":")[1];
					polTable[4]= aux.substring(0, aux.length()-3);
					if(mRNAaccId == ""){
						polTable[7] = source;
						polTable[17] = coordRelGene.split("\\.")[0];
						String aux2 = coordRelGene.split("\\.")[1];
						polTable[18] = aux2.substring(0, aux2.length()-2);
					}
				}
			}
	
			rs.close();
			pstmt.close();
		} catch(SQLException e) {
			System.out.println("SQL Exception... Error in querying:");
			e.printStackTrace();
		}
*/
		polTable[4] = "coord";
		polTable[7] = "source";
		polTable[17] = "coordRelGene1";
		polTable[18] = "coordRelGene2";
	}
}