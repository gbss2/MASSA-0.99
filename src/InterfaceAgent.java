import jade.content.ContentException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.wrapper.ContainerController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jsc.contingencytables.ContingencyTable2x2;
import jsc.contingencytables.FishersExactTest;
import BioData_Models.Polymorphism;

public class InterfaceAgent extends Agent{
	private static final long serialVersionUID = 1L;

	/* Attributes */
	private String servicetype;
	private String infotype;
	long start;
	long end;
	long exectime;
	public String slash;
	public String os;
	public String datapath;
	public String configpath;
	public String userdir;
	public String configfile;
	public String logfile;
	public String sumfile;
	public String sumfile2;
	private String datafilename;
	private String annotateddatafilename;
	private String enrichfile;
	private int analysistype;
	private int maxsizeforremoteannot;
	private int pquery;
	public List<String> rslist;
	public List<Integer> searchidlist;
	public List<BioData> biodatalist;
	BufferedWriter efileWriter;
	
	private int requestcount;
	public int rscount;
	public int annrscount;
	public boolean inputerror;
	public boolean sizeerror;
	public ContainerController cc;

	public String sn = "localhost";
	public String dbname = "massa";
	public String dbUser = "massa";
	public String dbKey = "@sdfghjkl!";
	MySQLcon myconnection;
	public Connection connection;

	/* Constructor */
	public InterfaceAgent(){
		Object[] args = getArguments();
		if (args != null) { cc = (ContainerController)args[0]; }

		rslist =  new ArrayList<String>();
		searchidlist =  new ArrayList<Integer>();
		biodatalist  =  new ArrayList<BioData>();
		requestcount = 0;
		rscount=0;
		annrscount=0;
		maxsizeforremoteannot = 100;
		
		setServicetype("Interface");
		setInfoType("Interface");

		inputerror = false;
		sizeerror = false;
		slash = System.getProperty("file.separator");
		os    = System.getProperty("os.name");
		this.userdir = System.getProperty("user.dir");
		this.datapath = userdir+slash+"data"+slash;
		this.configpath = userdir+slash+"config"+slash;

		this.configfile = this.configpath+"masconfig.txt";
	}

	/* Agent setup */
    protected void setup() {
        System.out.println("Agent "+getLocalName()+" started.");
		start = System.currentTimeMillis();
		System.out.println("Hello! MAS DiverEnrich is starting at "+start+" ..........");
        this.register();
        myconnection = new MySQLcon(sn, dbname, dbUser, dbKey);
        addBehaviour(new GetInputAction());
        addBehaviour(new waitMsg());
    }

	/* Agent shutdown */
	protected void takeDown() { System.out.println("Agent" + getLocalName() + " shutdown."); }
	
	/* Agent register */
	protected void register(){
    	try {
    		DFAgentDescription dfd = new DFAgentDescription();
    		dfd.setName(getAID());
			System.out.println("Agent "+getLocalName()+" registering service type \""+this.getServicetype()+"\" with DF");

			ServiceDescription sd = new ServiceDescription();
			sd.setName(this.getInfoType());
			sd.setType(this.getServicetype());
			dfd.addServices(sd);
    		DFService.register(this, dfd);
    	} catch (FIPAException fe) { fe.printStackTrace(); }
    }
	

	/* BEHAVIOURS */
	
	/* SEARCH PARTNER BEHAVIOUR */
	class FindAndSend extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private String type_of_service;
		private AID[] providers;
		private ACLMessage msg;
		private AID     msgreceiver;
		private String  msgperformative;
		private BioDataLite msgcontentlite;
		private BioData msgcontent;

		public FindAndSend(String st, String p, BioDataLite d) {
			type_of_service = st;
			msgperformative = p;
			msgcontentlite = d;
		}

		public FindAndSend(String st, String p, BioData d) {
			type_of_service = st;
			msgperformative = p;
			msgcontent = d;
		}

		public void action() {
			System.out.println("Agent "+getLocalName()+" searching for service type \""+type_of_service+"\"");
			try {
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription templateSd = new ServiceDescription();
				templateSd.setType(type_of_service);
				template.addServices(templateSd);

				DFAgentDescription[] results = DFService.search(myAgent, template);
				if (results.length > 0) {
					providers = new AID[results.length];
					for (int i = 0; i < results.length; ++i) {
						System.out.println("... found agent \""+results[i].getName());
						DFAgentDescription dfd = results[i];
						providers[i] = dfd.getName();
					}
				} else {
					System.out.println("Agent "+getLocalName()+" did not find any agent for the required information.");
					this.done();
				}
			} catch (FIPAException fe) { fe.printStackTrace(); }

			if(providers != null) {
				msgreceiver = providers[0];
				try {
					System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
					if (msgperformative == "REQUEST") { msg = new ACLMessage(ACLMessage.REQUEST); }
					msg.addReceiver(msgreceiver);
					msg.setLanguage("English");
					
					if(getAnalysistype()==1 || getAnalysistype()==3){ msg.setContentObject(msgcontentlite); }
					if(getAnalysistype()==2 || getAnalysistype()==4){ msg.setContentObject(msgcontent); }
					
					myAgent.send(msg);
					addRequestCount();
				} catch (IOException e) { e.printStackTrace(); }
			}
		}
	}
	
	private void addRequestCount(){
		this.requestcount++;
	}

	private void rmRequest(){
		this.requestcount--;
	}
	
	private int getRequestCount(){
		return this.requestcount;
	}	

	/* INITIATION BEHAVIOURS */
	
	/* GET INPUT ACTION - FINITE STATE MACHINE BEHAVIOUR */
	class GetInputAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		private static final String STATE_C = "C";
		private static final String STATE_D = "D";
		private static final String STATE_E = "E";


		public GetInputAction(){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new readConfigFile(), STATE_A);
			this.registerState(new readDataFile(), STATE_B);
			this.registerState(new preclearDB(), STATE_C);
			this.registerState(new sendAnnotationRequest(), STATE_D);
			this.registerLastState(new waitMsg(), STATE_E);
			
			this.registerDefaultTransition(STATE_A, STATE_B);
			this.registerDefaultTransition(STATE_B,STATE_C);
			this.registerDefaultTransition(STATE_C,STATE_D);
			this.registerDefaultTransition(STATE_D,STATE_E);

		}

		public int onEnd() { return super.onEnd(); }
	}
	
	/* GET INPUT ACTION - SUB BEHAVIOURS */
	/* READ CONFIGURATION FILE */	
	class readConfigFile extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		BufferedReader inputStream;
		String[] values;

		public readConfigFile() { }

		public void action() {
			System.out.println("Agent " + getLocalName() +" reading "+configfile+"...");

			try {
				inputStream = new BufferedReader(new FileReader(configfile));
				String l;
				while ((l = inputStream.readLine()) != null) {
					values = l.split("=");
					if(values[0].equals("analysis")){
						setAnalysistype(Integer.parseInt(values[1]));
					}
					if(values[0].equals("datafile")){
						setDatafilename(values[1]);
						setAnnotateddatafilename(values[1]+".annotated");
						setLogfileName(values[1]+".log");
						setSummaryfileName(values[1]+".sum");
						setSummaryfileDBName(values[1]+".DB.sum");
						setEnrichfileName(values[1]+".enrich");
					}
					if(values[0].equals("pquery")){
						setPquery(Integer.parseInt(values[1]));
					}
				}
			} catch (IOException e) { e.printStackTrace(); }
		}
	}	
	
	/* READ INPUT FILE */	
	class readDataFile extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		BufferedReader inputStream;
		String[] values;

		public readDataFile() { }

		public void action() {
			System.out.println("Agent " + getLocalName() +" reading "+datapath+datafilename+"...");
			
			try {
				inputStream = new BufferedReader(new FileReader(datapath+datafilename));
				String l;

				while ((l = inputStream.readLine()) != null) {
					if( !l.contains("rs")){
						inputerror = true;
						break;
					} else { if(!l.equals("")){ rslist.add(l); } }

					if(rslist.size() > maxsizeforremoteannot && (getAnalysistype()==2 || getAnalysistype()==4)){
						sizeerror = true;
						break;
					}
				}
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/* PRE-CLEAR ANNOTATION TABLES */
	class preclearDB extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		public preclearDB() { }

		public void action() {
			if(getAnalysistype()==1 || getAnalysistype()==3){
				System.out.println("Pre-clearing AnnotationDB...");
				
				try {
					connection = myconnection.mysqlConnect();
					Statement st = connection.createStatement();
	
				    ResultSet rs = st.executeQuery("SELECT table_name \"TABLES\" FROM information_schema.TABLES WHERE TABLES.TABLE_SCHEMA = 'annotation'");	
				    
					while (rs.next()) {
						String tablename = rs.getString("TABLES");
	
						if(!tablename.equals("gene")){
							if(!tablename.equals("snp")){
								if(!tablename.equals("search")){
									System.out.println("Deleting from "+tablename);
									String sql = "DELETE FROM "+tablename;
									Statement st1 = connection.createStatement();
									st1.executeUpdate(sql);
								}
							}
						}
					}
	
					rs.close();
	
					String sql = "DELETE FROM snp";
					st.executeUpdate(sql);
		
					String sql1 = "DELETE FROM gene WHERE genepk!=1";
					st.executeUpdate(sql1);
	
					String sql2 = "DELETE FROM search WHERE searchid!=1";
					st.executeUpdate(sql2);
	
					st.close();
					myconnection.mysqlDisconnect(); 
				} catch (SQLException e){ e.printStackTrace(); }
			}
		}
	}
	
	/* SEND ANNOTATION REQUEST TO COORDINATOR AGENT */
	class sendAnnotationRequest extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int rssizeperrequest;

		public sendAnnotationRequest(){ }

		public void action() {
			if(inputerror || sizeerror){
				if(inputerror){
					System.out.println("Agent " + getLocalName() +" is shutting down the platform due to WRONG SNP LIST INPUT FORMAT (EXISTING NON RS# SNPS)");
					ShutDownPlatform();
				}
				if(sizeerror){
					System.out.println("Agent " + getLocalName() +" is shutting down the platform due to TOO MANY SNPS FOR REMOTE ANNOTATION (>" +maxsizeforremoteannot+ "). TRY ANNOTATION TYPE 1 OR 3");
					ShutDownPlatform();
				}
			}else{	
				rscount = rslist.size();
				System.out.println("Agent " + getLocalName() +" sending "+rscount+" SNPs in "+getPquery()+" requests to Coordinator Agent.");
	
				double bouble = Math.ceil((double) rscount/getPquery());
				rssizeperrequest = (int) bouble;

				for (int i = 0; i < rscount; i += rssizeperrequest) {
					List<String> rssublist = new ArrayList<String>();
					rssublist = rslist.subList(i, Math.min(i + rssizeperrequest, rscount));
	
					String[] rsarray = new String[rssublist.size()];
	
					if(getAnalysistype()==1 || getAnalysistype()==3){
						BioDataLite inputdata = new BioDataLite();
						inputdata.setSnpRsList(rssublist.toArray(rsarray));
						inputdata.setAnalysistype(getAnalysistype());
						inputdata.setServicetype(0);
						System.out.println("Agent " + getLocalName() +" sending "+rssublist.size()+" SNPs to Coordinator Agent (Local).");
						addBehaviour(new FindAndSend("coordinator","REQUEST",inputdata));
					}
					
					if(getAnalysistype()==2 || getAnalysistype()==4){
						BioData inputdata = new BioData();
						inputdata.setSnpRsList(rssublist.toArray(rsarray));
						inputdata.setAnalysistype(getAnalysistype());
						System.out.println("Agent " + getLocalName() +" sending "+rssublist.size()+" SNPs to Coordinator Agent (Remote).");
						addBehaviour(new FindAndSend("coordinator","REQUEST",inputdata));
					}
				}
	
				rslist.clear();
			}
		}
		
		public void ShutDownPlatform(){
			Action actExpr = new Action();
			actExpr.setActor(getAMS());
			actExpr.setAction(new ShutdownPlatform());

			System.out.println("Agent " + getLocalName() +" shutting down the agent platform.");

			SLCodec codec = new SLCodec();
			getContentManager().registerOntology(JADEManagementOntology.getInstance());
			getContentManager().registerLanguage(codec, FIPANames.ContentLanguage.FIPA_SL0);
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.addReceiver(getAMS());
			request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
			request.setOntology(JADEManagementOntology.NAME);

			try {      
				getContentManager().fillContent(request, actExpr);
				send(request);
			} catch (ContentException ce) { ce.printStackTrace(); }
		}
	}
	
	/* WAIT COORDINATOR REPLY - CYCLIC BEHAVIOUR */
	class waitMsg extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		private AID sender;
		private String  msgperformative;

		public waitMsg() { }

		public void action() {
			ACLMessage msg = myAgent.receive();

			if (msg != null) {
				sender = msg.getSender();
				msgperformative = ACLMessage.getPerformative(msg.getPerformative());

				if(msgperformative == "INFORM" && !sender.getLocalName().equals("ams")){
						System.out.println("Agent " + getLocalName() +" received a "+msgperformative+" from agent "+sender.getName());

						try {
							System.out.println("Sender is " + sender.getLocalName());
							if (msg.getContentObject() instanceof BioData) {				
								BioData contentdata = (BioData)msg.getContentObject();
								biodatalist.add(contentdata);
								System.out.println("Agent " + getLocalName() +" biodatalist size is "+biodatalist.size()+" | request count is "+getRequestCount());
								if( biodatalist.size() == getRequestCount()){
									addBehaviour(new GenerateAnnotationReport());
								}
							}

							if (msg.getContentObject() instanceof BioDataLite) {	
								rmRequest();
								BioDataLite contentdata = (BioDataLite)msg.getContentObject();
								System.out.println("Agent " + getLocalName() +" received reply from " + sender.getName()+ " and service type is "+ contentdata.getServicetype());
								if(contentdata.getSearchidlist().size() > 0){
									Iterator<Integer> itr = contentdata.getSearchidlist().iterator();
									while(itr.hasNext()) {
									    Integer sid = (Integer)itr.next();
									    searchidlist.add(sid);
									}    
								}else{ searchidlist.add(contentdata.getSearchid()); }
								
								if(contentdata.getServicetype()==0) {	
									System.out.println("Agent " + getLocalName() +" generating annotation report ");
									addBehaviour(new GenerateAnnotationReport());
								}
								if(contentdata.getServicetype()==1) {
									System.out.println("Agent " + getLocalName() +" finishing annotation request ");
									addBehaviour(new FinishAnnotationReport());
								}
							}
						} catch (UnreadableException e) { e.printStackTrace(); }
				} else {
					System.out.println("Agent " + getLocalName() +" received a msg from "+sender.getName());
				}
			} else { block(); }
		}

	}

	
	/* ANNOTATION REPORT BEHAVIOURS */
	
	/* GENERATE ANNOTATION REPORT - FINITE STATE MACHINE BEHAVIOUR */	
	class GenerateAnnotationReport extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		private static final String STATE_C = "C";
		private static final String STATE_D = "D";


		public GenerateAnnotationReport(){
			System.out.println("Agent " + getLocalName() +" generating annotation report ...");
			
			if(getAnalysistype()==1 || getAnalysistype()==3){
				this.registerFirstState(new writeAnnotatedDB(), STATE_A);
				this.registerState(new writeSummary(), STATE_B);
				this.registerState(new sendEnrichmentRequest(), STATE_C);
				this.registerState(new waitMsg(), STATE_D);


				this.registerDefaultTransition(STATE_A, STATE_B);
				this.registerDefaultTransition(STATE_B, STATE_C);
				this.registerDefaultTransition(STATE_C, STATE_D);

			}

			if(getAnalysistype()==2 || getAnalysistype()==4){
				this.registerFirstState(new writeAnnotatedDataFile(), STATE_A);
				this.registerState(new writeSummary(), STATE_B);
				this.registerState(new sendEnrichmentRequest(), STATE_C);
				this.registerState(new waitMsg(), STATE_D);

				this.registerDefaultTransition(STATE_A, STATE_B);
				this.registerDefaultTransition(STATE_B, STATE_C);
				this.registerDefaultTransition(STATE_C, STATE_D);

			}
		}

		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished task.");
			return super.onEnd();
		}
	}	
	
	
	/* GENERATE ANNOTATION REPORT - LOCAL ANNOTATION SUB BEHAVIOURS */	
	
	/* WRITE ANNOTATION OUTPUT */	
	class writeAnnotatedDB extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		BufferedWriter fileWriter;
		PreparedStatement stdsnp;
		PreparedStatement stdGene;
		PreparedStatement stucsc;
		PreparedStatement streac;
		PreparedStatement stpgkb;
		PreparedStatement stgo;
		PreparedStatement stomim;
		PreparedStatement sthgnc;
		PreparedStatement stgwas;
		PreparedStatement stpph;
		PreparedStatement stprv;

		String querysnp;
		String queryGene;
		String queryucsc;
		String queryreac;
		String querypgkb;
		String querygo;
		String queryomim;
		String queryhgnc;
		String querygwas;
		String querypph;
		String queryprv;
		
		String polid;
		
		ResultSet rssnp;
		ResultSet rsgene;
		ResultSet rsucsc;
		ResultSet rsreac;
		ResultSet rspgkb;
		ResultSet rsgo;
		ResultSet rsomim;
		ResultSet rshgnc;
		ResultSet rsgwas;
		ResultSet rspph;
		ResultSet rsprv;
		
		int searchid;

		public writeAnnotatedDB() { }

		public void action() {
			System.out.println("Agent " + getLocalName() +" retrieving annotation from database...");

			try {
				fileWriter = new BufferedWriter(new FileWriter(datapath+annotateddatafilename));
			
				if(getAnalysistype()==3){
					fileWriter.write("PolymorphismId(1)\tPolymorphismType(2)\tGeneSymbol(3)\tGeneId(4)\tTranscriptRegion(5)\tNucleotideNumberingCodingDNA(6)\tChromosome(7)\tChromosomePosition(8)\tAncestralAllele(9)\tOrientation(10)\tAssemblyBuildVersion(11)\tAssemblyCoordStart(12)\tAssemblyCoordEnd(13)\tmRNAaccession(14)\tmRNAversion(15)\tAlleles(16)\tFrequency(17)\tStrand(18)\tRefUCSC(19)\tObservedUCSC(20)\tPolymorphismClass(21)\tFunctionalClass(22)\tReactomePathways(23)\tPGKB_Chemicals(24)\tPGKB_Lvl_Evidence(25)\tPGKB_Pharm_Phenotype(26)\tPGKB_Phenotypes(27)\tGOMolecularFunction(28)\tGOCellularComponent(29)\tGOBiologicalProcess(30)\tCytoloc(31)\tGeneStatus(32)\tGeneMapMethods(33)\tDisorders(34)\tMIMids(35)\tInheritance(36)\tPhenoMapMethods(37)\tComments(38)\tHgncId(39)\tGeneSymbol(40)\tGeneName(41)\tGeneSynonyms(42)\tLocusType(43)\tLocusGroup(44)\tGeneFamilyTag(45)\tGeneFamily(46)\tPubmed(47)\tReported_Genes(48)\tStrongest_SNP_Risk_Allele(49)\tContext(50)\tP-Value(51)\tDisease_Trait(52)\tSample_and_Population(53)\tPolyphenProteinID(54)\tPolyphenSubstitution(55)\tPolyphen2Prediction(56)\tPolyPhen2Prob(57)\tPolyphen2FDR(58)\tPolyphen1Prediction(59)\tProveanProteinID(60)\tProveanSubstitution(61)\tProveanProteinPos(62)\tProveanPrediction(63)\tProveanScore(64)\tSIFTPrediction(65)\tSIFTScore(66)");
					fileWriter.newLine();
				}
				if(getAnalysistype()==1){
					fileWriter.write("(1)Polymorphism Id \t (2)Polymorphism Type \t (3)Gene Symbol \t (4)Gene ID \t (5)Transcript Region \t (6)Nucleotide Numbering coding DNA \t (7)Chromosome \t (8)Chromosome Position \t (9)Ancestral Allele \t (10)Orientation \t (11)Assembly Build Version \t (12)Assembly Coord Start \t (13)Assembly Coord End \t  (14)mRNA accession \t (15)mRNA version \t (16)Alleles \t (17)Frequency");
					fileWriter.newLine();
				}

				connection = myconnection.mysqlConnect();
				Iterator<Integer> itr = searchidlist.iterator();
				
				while(itr.hasNext()) {
				    int id = (Integer)itr.next();
				  //  String querysnp = "SELECT * FROM snp,gene WHERE snp.fk_searchid=? AND gene.genepk=snp.fk_genepk";
				    String querysnp = "SELECT * FROM snp,gene WHERE snp.fk_searchid=? AND gene.genepk=snp.fk_genepk";
				    
				    stdsnp = connection.prepareStatement(querysnp);
				    stdsnp.setInt(1,id);
				    rssnp = stdsnp.executeQuery();

				    while (rssnp.next()) {
				    	String genesymbol = rssnp.getString(22);
				    	fileWriter.write("rs"+rssnp.getString(2)+
						    			 "\t"+rssnp.getString(3)+
						    			 "\t"+rssnp.getString(22)+
						    			 "\t"+rssnp.getString(23)+
						    			 "\t"+rssnp.getString(4)+
						    			 "\t"+rssnp.getString(7)+
						    			 "\t"+rssnp.getString(8)+
						    			 "\t"+rssnp.getString(9)+
						    			 "\t"+rssnp.getString(14)+
						    			 "\t"+rssnp.getString(15)+
						    			 "\t"+rssnp.getString(10)+
						    			 "\t"+rssnp.getString(11)+
						    			 "\t"+rssnp.getString(12)+
						    			 "\t"+rssnp.getString(16)+
						    			 "\t"+rssnp.getString(17)+
						    			 "\t"+rssnp.getString(5)+
						    			 "\t"+rssnp.getString(18));
	        			polid = "rs"+rssnp.getString(2);
		    	
				        if(getAnalysistype()==1){ fileWriter.newLine(); }

				        if(getAnalysistype()==3){
				        	queryucsc = "SELECT DISTINCT * FROM ucsc WHERE fk_searchid=? AND polID=?";
				        	stucsc = connection.prepareStatement(queryucsc);
				        	stucsc.setInt(1,id);
				        	stucsc.setString(2,polid);
				        	rsucsc = stucsc.executeQuery();
							
							if (!rsucsc.isBeforeFirst() ) {    
								fileWriter.write("\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null");
							} else {
								rsucsc.first();
								fileWriter.write("\t"+rsucsc.getString(2)+"\t"+rsucsc.getString(3)+"\t"+rsucsc.getString(4)+"\t"+rsucsc.getString(5)+"\t"+rsucsc.getString(6));
							}

							rsucsc.close();
							stucsc.close();

							queryreac = "SELECT DISTINCT * FROM reactome WHERE fk_searchid=? AND geneSymbol=?";
							streac = connection.prepareStatement(queryreac);
							streac.setInt(1,id);
							streac.setString(2,genesymbol);
							rsreac = streac.executeQuery();
							
							if (!rsreac.isBeforeFirst() ) {    
								fileWriter.write("\t"+"null");
							} else {
								rsreac.first();
								fileWriter.write("\t"+rsreac.getString(2));
							}

							rsreac.close();
							streac.close();

							querypgkb = "SELECT DISTINCT * FROM pharmGKB_snp WHERE fk_searchid=? AND snpid=?";
							stpgkb = connection.prepareStatement(querypgkb);
							stpgkb.setInt(1,id);
							stpgkb.setString(2,polid);
							rspgkb = stpgkb.executeQuery();

							if (!rspgkb.isBeforeFirst() ) {    
								fileWriter.write("\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null");
							} else {
								rspgkb.first();
								fileWriter.write("\t"+rspgkb.getString(2)+"\t"+rspgkb.getString(3)+"\t"+rspgkb.getString(4)+"\t"+rspgkb.getString(5));
							}

							rspgkb.close();
							stpgkb.close();

							querygo = "SELECT DISTINCT * FROM geneOntology WHERE fk_searchid=? AND gp_symbol=?";
							stgo = connection.prepareStatement(querygo);
							stgo.setInt(1,id);
							stgo.setString(2,genesymbol);
							rsgo = stgo.executeQuery();

							if (!rsgo.isBeforeFirst() ) {    
								fileWriter.write("\t"+"null"+"\t"+"null"+"\t"+"null");
							} else {
								rsgo.first();
								fileWriter.write("\t"+rsgo.getString(3)+"\t"+rsgo.getString(4)+"\t"+rsgo.getString(5));
							}

							rsgo.close();
							stgo.close();

							queryomim = "SELECT DISTINCT * FROM omim WHERE fk_searchid=? AND gene_symbol=?";
							stomim = connection.prepareStatement(queryomim);
							stomim.setInt(1,id);
							stomim.setString(2,genesymbol);
							rsomim = stomim.executeQuery();

							if (!rsomim.isBeforeFirst() ) {    
								fileWriter.write("\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null");
							} else {
								rsomim.first();
								fileWriter.write("\t"+rsomim.getString(3)+"\t"+rsomim.getString(4)+"\t"+rsomim.getString(5)+"\t"+rsomim.getString(6)+"\t"+rsomim.getString(7)+"\t"+"null"+"\t"+rsomim.getString(12)+"\t"+rsomim.getString(8));
							}

							rsomim.close();
							stomim.close();
							
							queryhgnc = "SELECT DISTINCT * FROM hugoDB WHERE fk_searchid=? AND symbol =?";
							sthgnc = connection.prepareStatement(queryhgnc);
							sthgnc.setInt(1,id);
							sthgnc.setString(2,genesymbol);
							rshgnc = sthgnc.executeQuery();

							if (!rshgnc.isBeforeFirst() ) {    
								fileWriter.write("\t"+ "null" +"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t" +"null"+"\t"+"null"+"\t"+
								"null" +"\t"+"null");
							} else {
								rshgnc.first();
								fileWriter.write("\t"+ rshgnc.getString(2) +"\t"+rshgnc.getString(3)+"\t"+rshgnc.getString(4)+"\t"+rshgnc.getString(10)+"\t" +rshgnc.getString(6)+"\t"+rshgnc.getString(7)+"\t"+
								rshgnc.getString(26) +"\t"+rshgnc.getString(27));
							}

							rshgnc.close();
							sthgnc.close();

							querygwas = "SELECT DISTINCT * FROM gwascatalog WHERE fk_searchid=? AND snps=?";
							stgwas = connection.prepareStatement(querygwas);
							stgwas.setInt(1,id);
							stgwas.setString(2,polid);
							rsgwas = stgwas.executeQuery();

							if (!rsgwas.isBeforeFirst() ) {    
								fileWriter.write("\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null");
							} else {
								rsgwas.first();
								fileWriter.write("\t"+rsgwas.getString(3)+"\t"+rsgwas.getString(4)+"\t"+rsgwas.getString(5)+"\t"+rsgwas.getString(6)+"\t"+rsgwas.getString(9)+"\t"+rsgwas.getString(7)+"\t"+rsgwas.getString(8));
							}

							rsgwas.close();
							stgwas.close();

							querypph = "SELECT DISTINCT * FROM polyphen WHERE fk_searchid=? AND rsid=?";
							stpph = connection.prepareStatement(querypph);
							stpph.setInt(1,id);
							stpph.setString(2,polid);
							rspph = stpph.executeQuery();

							if (!rspph.isBeforeFirst() ) {    
								fileWriter.write("\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null");
							} else {
								rspph.first();
								fileWriter.write("\t"+rspph.getString(2)+"\t"+rspph.getString(3)+"\t"+rspph.getString(5)+"\t"+rspph.getString(6)+"\t"+rspph.getString(7)+"\t"+rspph.getString(8));
							}
								
							rspph.close();
							stpph.close();

							queryprv = "SELECT DISTINCT * FROM provean WHERE fk_searchid=? AND rsid=?";
							stprv = connection.prepareStatement(queryprv);
							stprv.setInt(1,id);
							stprv.setString(2,polid);
							rsprv = stprv.executeQuery();

							if (!rsprv.isBeforeFirst() ) {    
								fileWriter.write("\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null"+"\t"+"null");
							} else {
								rsprv.first();
								fileWriter.write("\t"+rsprv.getString(2)+"\t"+rsprv.getString(3)+"\t"+rsprv.getString(6)+"\t"+rsprv.getString(4)+"\t"+rsprv.getString(5)+"\t"+rsprv.getString(8)+"\t"+rsprv.getString(7));
							}

							rsprv.close();
							stprv.close();

				        	fileWriter.newLine();
				        }
				    }

					rssnp.close();
					stdsnp.close();
				}

				fileWriter.close();
			} catch (IOException e)  { 
				e.printStackTrace(); 
			}
			  catch (SQLException e) { 
				  e.printStackTrace(); 
			} 
		}
		
		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished writing annotated report on "+datapath+annotateddatafilename);
			myconnection.mysqlDisconnect();
			end = System.currentTimeMillis();
			exectime = (end - start)/1000;
			System.out.println("TOTAL ANNOTATION TIME: "+exectime+"s");

			return 0;
		}
	}

	/* SUMMARIZE AND WRITE SUMMARY OUTPUT */
	class writeSummary extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		BufferedReader inputStream;
		BufferedWriter fileWriter;
		String[] values;
		String[] values2;
		int gfTagSize = 0;
		int transRegSize = 0;
		int functionSize = 0;
		int pgkbPathSize = 0;
		int pgkbDiseaseSize = 0;
		int pgkbDrugSize = 0;
		int omimDisorderSize = 0;
		int omimCytoLocSize = 0;
		int goBioPSize = 0;
		int goCelCSize = 0;
		int goMolFSize = 0;
		int reactomePathSize = 0;

		Map<String, MutableInt> geneNameCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> fxnCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> chrCount = new HashMap<String, MutableInt>();
		
		Map<String, MutableInt> functionCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> pathwayCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> drugCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> diseaseCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> goMolFunCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> goCelCompCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> goBioProCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> cytoLocCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> disorderCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> locTypeCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> gfTagCount = new HashMap<String, MutableInt>();
		Map<String, MutableInt> reactomePathCount = new HashMap<String, MutableInt>();

		public writeSummary(){ }
		
		public void action(){
			System.out.println("Agent " + getLocalName() +" writing annotation summary at "+ sumfile + "...");

			int curLine = 0;
			try {
				inputStream = new BufferedReader(new FileReader(datapath+annotateddatafilename));
				String l;
				while ((l = inputStream.readLine()) != null) {
				   if (curLine++ < 1) {
			            continue;
			        }
			
					values = l.split("\t");
					String gn = values[2];
					String tr = values[4];
					String ch = values[6];
					
					MutableInt count1 = geneNameCount.get(gn);
					if (count1 == null) {
					    geneNameCount.put(gn, new MutableInt());
					} else { count1.increment(); }

					MutableInt count2 = fxnCount.get(tr);
					if (count2 == null) {
					    fxnCount.put(tr, new MutableInt());
					} else { count2.increment(); }
					
					MutableInt count3 = chrCount.get(ch);
					if (count3 == null) {
					    chrCount.put(ch, new MutableInt());
					} else { count3.increment(); }
				
					if(getAnalysistype()==4 || getAnalysistype()==3){
						String fun = values[21];
						String rpath = values[22];
						String path = values[23];
						String drug = values[24];
						String dise = values[25];
						String gomf = values[27];
						String gocc = values[28];
						String gobp = values[29];
						String cyto = values[30];
						String diso = values[33];
						String locT = values[42];
						String gfTag = values[44];
		
						MutableInt count4 = functionCount.get(fun);
						if (count4 == null) {
						    functionCount.put(fun, new MutableInt());
						} else { count4.increment(); }
		
						values2 = path.split(";");
						for(String value : values2) {
							MutableInt count5 = pathwayCount.get(value);
							if (count5 == null) {
								pathwayCount.put(value, new MutableInt());
								pathwayCount.get(value).addGene(gn);
							} else {
								count5.increment();
								pathwayCount.get(value).addGene(gn);
							}
						}
		
						values2 = drug.split(";");
						for(String value : values2) {
							MutableInt count6 = drugCount.get(value);
							if (count6 == null) {
								drugCount.put(value, new MutableInt());
								drugCount.get(value).addGene(gn);
							} else {
							    count6.increment();
							    drugCount.get(value).addGene(gn);
							}
						}
		
						values2 = dise.split(";");
						for(String value : values2) {
							MutableInt count7 = diseaseCount.get(value);
							if (count7 == null) {
								diseaseCount.put(value, new MutableInt());
								diseaseCount.get(value).addGene(gn);
							} else {
							    count7.increment();
							    diseaseCount.get(value).addGene(gn);
							}
						}
		
						values2 = gomf.split(";");
						for(String value : values2) {
							MutableInt count8 = goMolFunCount.get(value);
							if (count8 == null) {
								goMolFunCount.put(value, new MutableInt());
								goMolFunCount.get(value).addGene(gn);
							} else {
							    count8.increment();
							    goMolFunCount.get(value).addGene(gn);
							}
						}
		
						values2 = gocc.split(";");
						for(String value : values2) {
							MutableInt count9 = goCelCompCount.get(value);
							if (count9 == null) {
								goCelCompCount.put(value, new MutableInt());
								goCelCompCount.get(value).addGene(gn);
							} else {
							    count9.increment();
							    goCelCompCount.get(value).addGene(gn);
							}
						}
		
						values2 = gobp.split(";");
						for(String value : values2) {
							MutableInt count10 = goBioProCount.get(value);
							if (count10 == null) {
								goBioProCount.put(value, new MutableInt());
								goBioProCount.get(value).addGene(gn);
							} else {
							    count10.increment();
							    goBioProCount.get(value).addGene(gn);
							}
						}
		
						MutableInt count11 = cytoLocCount.get(cyto);
						if (count11 == null) {
							cytoLocCount.put(cyto, new MutableInt());
							cytoLocCount.get(cyto).addGene(gn);
						} else {
						    count11.increment();
						    cytoLocCount.get(cyto).addGene(gn);
						}
							
						MutableInt count13 = locTypeCount.get(locT);
						if (count13 == null) {
							locTypeCount.put(locT, new MutableInt());
							locTypeCount.get(locT).addGene(gn);
						} else {
						    count13.increment();
						    locTypeCount.get(locT).addGene(gn);
						}
		
						MutableInt count15 = gfTagCount.get(gfTag);
						if (count15 == null) {
							gfTagCount.put(gfTag, new MutableInt());
							gfTagCount.get(gfTag).addGene(gn);
						} else {
						    count15.increment();
						    gfTagCount.get(gfTag).addGene(gn);
						}
		
						values2 = diso.split(";");
						for(String value : values2) {
							MutableInt count12 = disorderCount.get(value);
							if (count12 == null) {
								disorderCount.put(value, new MutableInt());
								disorderCount.get(value).addGene(gn);
							} else {
								count12.increment();
								disorderCount.get(value).addGene(gn);
							}
						}
						
						values2 = rpath.split(";");
						for(String value : values2) {
							MutableInt count16 = reactomePathCount.get(value);
							if (count16 == null) {
								reactomePathCount.put(value, new MutableInt());
								reactomePathCount.get(value).addGene(gn);
							} else {
								count16.increment();
								reactomePathCount.get(value).addGene(gn);
							}
						}
					}
				}
				inputStream.close();
			} catch (IOException e) { e.printStackTrace(); }

			try{
				fileWriter = new BufferedWriter(new FileWriter(sumfile));
				Iterator<Entry<String, MutableInt>> itr0 = geneNameCount.entrySet().iterator();
				while(itr0.hasNext()){
					Map.Entry<String, MutableInt> entry = itr0.next();
				    fileWriter.write("dbSNP Gene Symbol \t" +entry.getKey()+"\t"+entry.getValue().get()+"\tnull\n");	
				}

				Iterator<Entry<String, MutableInt>> itr2 = fxnCount.entrySet().iterator();
				while(itr2.hasNext()){
					Map.Entry<String, MutableInt> entry = itr2.next();
					fileWriter.write("dbSNP Transcript Region \t"+entry.getKey()+"\t"+entry.getValue().get()+"\tnull\n");
					transRegSize = transRegSize + Integer.valueOf(entry.getValue().get());
				}
				
				Iterator<Entry<String, MutableInt>> itr3 = chrCount.entrySet().iterator();
				while(itr3.hasNext()){
					Map.Entry<String, MutableInt> entry = itr3.next();
					fileWriter.write("dbSNP Chromosome \t"+entry.getKey()+"\t"+entry.getValue().get()+"\tnull\n");
				}

				if(getAnalysistype()==4 || getAnalysistype()==3){
					Iterator<Entry<String, MutableInt>> itr4 = functionCount.entrySet().iterator();
					while(itr4.hasNext()){
						Map.Entry<String, MutableInt> entry = itr4.next();
					    fileWriter.write("UCSC Transcript region \t"+entry.getKey()+"\t"+entry.getValue().get()+"\tnull\n");	
					}
					
					Iterator<Entry<String, MutableInt>> itr16 = reactomePathCount.entrySet().iterator();
					while(itr16.hasNext()){
						Map.Entry<String, MutableInt> entry = itr16.next();
					    fileWriter.write("Reactome Pathway\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    reactomePathSize = reactomePathSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr5 = pathwayCount.entrySet().iterator();
					while(itr5.hasNext()){
						Map.Entry<String, MutableInt> entry = itr5.next();
					    fileWriter.write("PharmGKB Pathway\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");	
					    pgkbPathSize = pgkbPathSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr6 = drugCount.entrySet().iterator();
					while(itr6.hasNext()){
						Map.Entry<String, MutableInt> entry = itr6.next();
					    fileWriter.write("PharmGKB Drugs\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    pgkbDrugSize = pgkbDrugSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr7 = diseaseCount.entrySet().iterator();
					while(itr7.hasNext()){
						Map.Entry<String, MutableInt> entry = itr7.next();
					    fileWriter.write("PharmGKB Disease\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    pgkbDiseaseSize = pgkbDiseaseSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr8 = goMolFunCount.entrySet().iterator();
					while(itr8.hasNext()){
						Map.Entry<String, MutableInt> entry = itr8.next();
					    fileWriter.write("GO Molecular Function\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    goMolFSize = goMolFSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr9 = goCelCompCount.entrySet().iterator();
					while(itr9.hasNext()){
						Map.Entry<String, MutableInt> entry = itr9.next();
					    fileWriter.write("GO Cellular Component\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    goCelCSize = goCelCSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr10 = goBioProCount.entrySet().iterator();
					while(itr10.hasNext()){
						Map.Entry<String, MutableInt> entry = itr10.next();
					    fileWriter.write("GO Biological Process\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    goBioPSize = goBioPSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr11 = cytoLocCount.entrySet().iterator();
					while(itr11.hasNext()){
						Map.Entry<String, MutableInt> entry = itr11.next();
					    fileWriter.write("OMIM CytoLocation\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    omimCytoLocSize = omimCytoLocSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr12 = disorderCount.entrySet().iterator();
					while(itr12.hasNext()){
						Map.Entry<String, MutableInt> entry = itr12.next();
					    fileWriter.write("OMIM Disorders\t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    omimDisorderSize = omimDisorderSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

					Iterator<Entry<String, MutableInt>> itr13 = locTypeCount.entrySet().iterator();
					while(itr13.hasNext()){
						Map.Entry<String, MutableInt> entry = itr13.next();
					    fileWriter.write("HGNC Locus Type \t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");	
					}

					Iterator<Entry<String, MutableInt>> itr15 = gfTagCount.entrySet().iterator();
					while(itr15.hasNext()){
						Map.Entry<String, MutableInt> entry = itr15.next();
					    fileWriter.write("HGNC Gene Family \t"+entry.getKey()+"\t"+entry.getValue().get()+"\t"+entry.getValue().getGeneCount()+"\n");
					    gfTagSize = gfTagSize + Integer.valueOf(entry.getValue().getGeneCount());
					}

/*					System.out.println("ENRICHMENT TEST REMOTE");
					
					efileWriter = new BufferedWriter(new FileWriter(enrichfile));
					efileWriter.write("Database\tTerm\tp-Value\tGene Population Count\tSNP Count\tGene Sample Count\tGene List\n");
					
					BioEnrichment hgncCount = new BioEnrichment();
					hgncCount.setHgncGeneFamilyCount();
					System.out.println("Enrichment Test GeneFamily - Sample Matrix Size = "+ gfTagCount.size());
					EnrichmentTestRemote("HGNC Gene Family",gfTagCount,hgncCount.getHgncGeneFamilyCount(),gfTagSize);
					
					BioEnrichment edisorderCount = new BioEnrichment();

					edisorderCount.setomimMorbidMapCount1();
					edisorderCount.setomimMorbidMapCount2();
					System.out.println("Enrichment Test OmimDisorder - Sample Matrix Size = "+ disorderCount.size());
					EnrichmentTestRemote("OMIM MorbidMap Disorder",disorderCount,edisorderCount.getomimMorbidMapCount(),omimDisorderSize);
					
					BioEnrichment ecytoLocCount = new BioEnrichment();
					ecytoLocCount.setomimCytoLocCount();
					System.out.println("Enrichment Test CytoLoc - Sample Matrix Size = "+ disorderCount.size());
					EnrichmentTestRemote("OMIM CytoLoc",cytoLocCount,ecytoLocCount.getomimCytoLocCount(),omimCytoLocSize);

					BioEnrichment etransRegCount = new BioEnrichment();
					etransRegCount.setdbsnpTransRegCount();
					BioEnrichment egoMolFunCount = new BioEnrichment();
					egoMolFunCount.setgoMolFunctionCount();
					System.out.println("Enrichment Test GOMolFunction - Sample Matrix Size = "+ goMolFunCount.size());
					EnrichmentTestRemote("GO Molecular Function",goMolFunCount,egoMolFunCount.getgoMolFunctionCount(),goMolFSize);
					
					BioEnrichment egoBioPCount = new BioEnrichment();
					egoBioPCount.setgoBioProcessCount1();
					egoBioPCount.setgoBioProcessCount2();
					egoBioPCount.setgoBioProcessCount3();
					System.out.println("Enrichment Test GOBioProcess - Sample Matrix Size = "+ goBioProCount.size());
					EnrichmentTestRemote("GO Bio Process",goBioProCount,egoBioPCount.getgoBioProcessCount(),goBioPSize);

					BioEnrichment egoCelCompCount = new BioEnrichment();
					egoCelCompCount.setgoCelCompCount();
					System.out.println("Enrichment Test GOCelComp - Sample Matrix Size = "+ goCelCompCount.size());
					EnrichmentTestRemote("GO Cellular Component",goCelCompCount,egoCelCompCount.getgoCelCompCount(),goCelCSize);

					BioEnrichment erpathwayCount = new BioEnrichment();
					erpathwayCount.setreactomePathwayCount();
					System.out.println("Enrichment Test Reactome Pathway - Sample Matrix Size = "+ reactomePathCount.size());
					EnrichmentTestRemote("Reactome Pathways",reactomePathCount,erpathwayCount.getreactomePathwayCount(),reactomePathSize);
	
					BioEnrichment2 epathwayCount = new BioEnrichment2();
					epathwayCount.setpgkbPathwayCount();
					System.out.println("Enrichment Test PGKB Pathway - Sample Matrix Size = "+ pathwayCount.size());
					EnrichmentTestRemote("PharmGKB Pathways",pathwayCount,epathwayCount.getpgkbPathwayCount(),pgkbPathSize);
					
					BioEnrichment2 edrugCount = new BioEnrichment2();
					edrugCount.setpgkbDrugCount();
					System.out.println("Enrichment Test PGKB Drug - Sample Matrix Size = "+ drugCount.size());
					EnrichmentTestRemote("PharmGKB Drugs",drugCount,edrugCount.getpgkbDrugCount(),pgkbDrugSize);

					BioEnrichment2 ediseaseCount = new BioEnrichment2();
					ediseaseCount.setpgkbDiseaseCount();
					System.out.println("Enrichment Test PGKB Disease - Sample Matrix Size = "+ diseaseCount.size());
					EnrichmentTestRemote("PGKB Disease",diseaseCount,ediseaseCount.getpgkbDiseaseCount(),pgkbDiseaseSize);

					efileWriter.close(); */
				}
						
				fileWriter.close();
			} catch (IOException e) { e.printStackTrace(); }	
		}
	}
	
	/* SEND SEA ENRICHMENT REQUEST TO COORDINATOR AGENT */
	class sendEnrichmentRequest extends OneShotBehaviour {
		
		private static final long serialVersionUID = 1L;
		
		public sendEnrichmentRequest(){ }
		
		public void action() {
			
			if(getAnalysistype()==2 || getAnalysistype()==4){

				BioDataLite enrichData = new BioDataLite();
				enrichData.setFullFilename(annotateddatafilename);
				enrichData.setAnalysistype(getAnalysistype());
				enrichData.setServicetype(1);
				System.out.println("Agent " + getLocalName() +" sending SEA request to Coordinator Agent (Remote).");
				addBehaviour(new FindAndSend("coordinator","REQUEST",enrichData));
				
			} 
			
			if(getAnalysistype()==1 || getAnalysistype()==3) {
				
				BioDataLite enrichData = new BioDataLite();
				enrichData.setFullFilename(annotateddatafilename);
				enrichData.setAnalysistype(getAnalysistype());
				enrichData.setServicetype(1);
				System.out.println("Agent " + getLocalName() +" sending SEA REQUEST to Coordinator Agent (Local).");
				System.out.println("Agent " + getLocalName() +" Analysis Type = "+ enrichData.getAnalysistype() +" Service Type = "+ enrichData.getServicetype() +" Path = "+ enrichData.getFullFilename() );
				addBehaviour(new FindAndSend("coordinator","REQUEST",enrichData));	
				
			}
		}
		
	}
	
	
	
	/* GENERATE ANNOTATION REPORT - REMOTE ANNOTATION SUB BEHAVIOURS */	
	
	/* WRITE ANNOTATION OUTPUT */
	
	class writeAnnotatedDataFile extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		BufferedWriter fileWriter;
		String[] values;

		public writeAnnotatedDataFile() { }

		public void action() {
			try {
				fileWriter = new BufferedWriter(new FileWriter(datapath+annotateddatafilename));

				if(getAnalysistype()==2){
					fileWriter.write("PolymorphismId(1)\tPolymorphismType(2)\tGeneSymbol(3)\tGeneId(4)\tTranscriptRegion(5)\tNucleotideNumberingCodingDNA(6)\tChromosome(7)\tChromosomePosition(8)\tAncestralAllele(9)\tOrientation(10)\tAssemblyBuildVersion(11)\tAssemblyCoordStart(12)\tAssemblyCoordEnd(13)\tmRNAaccession(14)\tmRNAversion(15)\tAlleles(16)\tFrequency(17)");
					fileWriter.newLine();
					
					Iterator<BioData> itr = biodatalist.iterator();
					while(itr.hasNext()) {
					    BioData annotateddata = (BioData)itr.next();
						for (Polymorphism p : annotateddata.getPolymorphismList()){
							annrscount++;
							fileWriter.write("rs"+p.getPolymorphismCode()+"\t"+p.getKind()+"\t"+p.getGenesymbol()+"\t"+p.getGeneid()+"\t"+p.getSubKind()+"\t"+p.getCoordRelGene()+"\t"+p.getChromosome()+"\t"+p.getCoordRefSeq()+"\t"+p.getAncestralAllele()+"\t"+p.getOrientation()+"\t"+p.getAssm_build_version()+"\t"+p.getAssm_coord_start()+"\t"+p.getAssm_coord_end()+"\t"+p.getMrnaAcc()+"\t"+p.getMrnaVer()+"\t"+p.getReferenceAllele()+"\t"+p.getFreq());
							fileWriter.newLine();
						}
					}
				}

				if(getAnalysistype()==4){
					fileWriter.write("PolymorphismId(1)\tPolymorphismType(2)\tGeneSymbol(3)\tGeneId(4)\tTranscriptRegion(5)\tNucleotideNumberingCodingDNA(6)\tChromosome(7)\tChromosomePosition(8)\tAncestralAllele(9)\tOrientation(10)\tAssemblyBuildVersion(11)\tAssemblyCoordStart(12)\tAssemblyCoordEnd(13)\tmRNAaccession(14)\tmRNAversion(15)\tAlleles(16)\tFrequency(17)\tStrand(18)\tRefUCSC(19)\tObservedUCSC(20)\tPolymorphismClass(21)\tFunctionalClass(22)\tPathways(23)\tDrugs(24)\tDisease(25)\tRelatedGenes(26)\tGOMolecularFunction(27)\tGOCellularComponent(28)\tGOBiologicalProcess(29)\tCytoloc(30)\tGeneStatus(31)\tGeneMapMethods(32)\tDisorders(33)\tMIMids(34)\tInheritance(35)\tPhenoMapMethods(36)\tComments(37)\tHgncId(38)\tGeneName(39)\tGeneSynonyms(40)\tLocusType(41)\tLocusGroup(42)\tGeneFamilyTag(43)\tGeneFamily(44)");
					fileWriter.newLine();
					
					Iterator<BioData> itr = biodatalist.iterator();
					while(itr.hasNext()) {
					    BioData annotateddata = (BioData)itr.next();
					    for (Polymorphism p : annotateddata.getPolymorphismList()){
					    	annrscount++;
					    	fileWriter.write("rs"+p.getPolymorphismCode()+"\t"+p.getKind()+"\t"+p.getGenesymbol()+"\t"+p.getGeneid()+"\t"+p.getSubKind()+"\t"+p.getCoordRelGene()+"\t"+p.getChromosome()+"\t"+p.getCoordRefSeq()+"\t"+p.getAncestralAllele()+"\t"+p.getOrientation()+"\t"+p.getAssm_build_version()+"\t"+p.getAssm_coord_start()+"\t"+p.getAssm_coord_end()+"\t"+p.getMrnaAcc()+"\t"+p.getMrnaVer()+"\t"+p.getReferenceAllele()+"\t"+p.getFreq()+"\t"+p.getStrand()+"\t"+p.getRefUCSC()+"\t"+p.getObsGen()+"\t"+p.getUcscClass()+"\t"+p.getUcscFunc()+"\t");

					    	Iterator<String> itp = p.getPathwayList().iterator();
					    	while(itp.hasNext()) {
					    		String pw = (String)itp.next();
					    		fileWriter.write(pw+";");
					    	}

					    	if(p.getPathwayList().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itd = p.getDrugList().iterator();
					    	while(itd.hasNext()) {
					    		String dg = (String)itd.next();
					    		fileWriter.write(dg+";");
					    	}

					    	if(p.getDrugList().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> its = p.getDiseaseList().iterator();
					    	while(its.hasNext()) {
					    		String ds = (String)its.next();
					    		fileWriter.write(ds+";");
					    	}

					    	if(p.getDiseaseList().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itg = p.getGenexList().iterator();
					    	while(itg.hasNext()) {
					    		String gx = (String)itg.next();
					    		fileWriter.write(gx+";");
					    	}

					    	if(p.getGenexList().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itmf = p.getMolFunction().iterator(); 
					    	while(itmf.hasNext()) {
					    		String mf = (String)itmf.next();
					    		fileWriter.write(mf+";");
					    	}

					    	if(p.getMolFunction().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itcc = p.getCelComponent().iterator(); 
					    	while(itcc.hasNext()) {
					    		String cc = (String)itcc.next();
					    		fileWriter.write(cc+";");
					    	}
					    	
					    	if(p.getCelComponent().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itbp = p.getBioProcess().iterator();
					    	while(itbp.hasNext()) {
					    		String bp = (String)itbp.next();
					    		fileWriter.write(bp+";");
					    	}

					    	if(p.getBioProcess().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");
					    	fileWriter.write(p.getCytoloc()+"\t"+p.getGenestatus()+"\t"+p.getGenemapmethods()+"\t");

					    	Iterator<String> itdo = p.getDisorder().iterator();
					    	while(itdo.hasNext()) {
					    		String dor = (String)itdo.next();
					    		fileWriter.write(dor+";");
					    	}
					    	
					    	if(p.getDisorder().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itmim = p.getMimID().iterator();
					    	while(itmim.hasNext()) {
					    		String mim = (String)itmim.next();
					    		fileWriter.write(mim+";");
					    	}
					    	
					    	if(p.getMimID().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itinh = p.getInheritance().iterator();
					    	while(itinh.hasNext()) {
					    		String inh = (String)itinh.next();
					    		fileWriter.write(inh+";");
					    	}

					    	if(p.getInheritance().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itpmm = p.getPhenoMapMethods().iterator();
					    	while(itpmm.hasNext()) {
					    		String pmm = (String)itpmm.next();
					    		fileWriter.write(pmm+";");
					    	}

					    	if(p.getPhenoMapMethods().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");

					    	Iterator<String> itcom = p.getComments().iterator();
					    	while(itcom.hasNext()) {
					    		String com = (String)itcom.next();
					    		fileWriter.write(com+";");
					    	}

					    	if(p.getComments().size() == 0){ fileWriter.write("null"); }
					    	fileWriter.write("\t");
					    	fileWriter.write(p.getHgncId()+"\t"+p.getHgGeneName()+"\t"+p.getGeneSynonyms()+"\t"+p.getLocusType()+"\t"+p.getLocusGroup()+"\t"+p.getGeneFamilyTag()+"\t"+p.getGeneFamily());
					    	fileWriter.newLine();
					    }
					}
				}
				
				fileWriter.close();
			} catch (IOException e) { e.printStackTrace(); }
		}

		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished writing annotated file on "+datapath+annotateddatafilename);
			end = System.currentTimeMillis();
			exectime = (end - start)/1000;
			System.out.println("TOTAL ANNOTATION TIME: "+exectime+"s");

			return 0;
		}
	}
	
	
	/* FINISH ANNOTATION BEHAVIOURS */
	
	/* FINISH ANNOTATION TASK - FINITE STATE MACHINE BEHAVIOUR */	
	class FinishAnnotationReport extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		private static final String STATE_C = "C";

		public FinishAnnotationReport(){
			System.out.println("Agent " + getLocalName() +" will shutdown platform ...");
			
			if(getAnalysistype()==1 || getAnalysistype()==3){
				this.registerFirstState(new cleanDB(), STATE_A);
				this.registerState(new writeLogFile(), STATE_B);
				this.registerLastState(new ShutDownPlatform(), STATE_C);

				this.registerDefaultTransition(STATE_A, STATE_B);
				this.registerDefaultTransition(STATE_B, STATE_C);

			}

			if(getAnalysistype()==2 || getAnalysistype()==4){
				this.registerFirstState(new clearBDObjects(), STATE_A);
				this.registerState(new writeLogFile(), STATE_B);
				this.registerLastState(new ShutDownPlatform(), STATE_C);

				this.registerDefaultTransition(STATE_A, STATE_B);
				this.registerDefaultTransition(STATE_B, STATE_C);

			}
		}

		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished task.");
			return super.onEnd();
		}
	}
	
	/* CLEAN ANNOTATION TABLES */
	class cleanDB extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		public cleanDB() { }

		public void action() {
			System.out.println("Agent " + getLocalName() +" cleanning up temporary annotation on local database.");

			try {
				connection = myconnection.mysqlConnect();
				Statement st = connection.createStatement();
				
				Iterator<Integer> itr = searchidlist.iterator();
				while(itr.hasNext()) {
				    int id = (Integer)itr.next();
				    ResultSet rs = st.executeQuery("SELECT table_name \"TABLES\" FROM information_schema.TABLES WHERE TABLES.TABLE_SCHEMA = 'annotation'");	
	
				    while (rs.next()) {
						String tablename = rs.getString("TABLES");
	
						if(!tablename.equals("gene")){
							if(!tablename.equals("snp")){
								if(!tablename.equals("search")){
									System.out.println("Deleting from "+tablename);
									String sql = "DELETE FROM "+tablename;
									Statement st1 = connection.createStatement();
									st1.executeUpdate(sql);
									st1.close();
								}
							}
						}
					}
	
				    rs.close();
	
					String sql = "DELETE FROM snp WHERE fk_searchid=?";
					PreparedStatement snpDelete = connection.prepareStatement(sql);
					snpDelete.setInt(1,id);
					int deletecount = snpDelete.executeUpdate();
					snpDelete.close();
					
					annrscount = annrscount + deletecount;
					
					String sql1 = "DELETE FROM gene WHERE genepk!=1 AND fk_searchid=?";
					PreparedStatement geneDelete = connection.prepareStatement(sql1);
					geneDelete.setInt(1,id);
					geneDelete.executeUpdate();
					geneDelete.close();
					
					String sql2 = "DELETE FROM search WHERE searchid=?";
					PreparedStatement searchDelete = connection.prepareStatement(sql2);
					searchDelete.setInt(1,id);
					searchDelete.executeUpdate();
					searchDelete.close();
				}
	
				st.close();
				myconnection.mysqlDisconnect();
			} catch (SQLException e){ e.printStackTrace(); }
		}
	}
	
	/* CLEAN ANNOTATION OBJECTS */
	
	class clearBDObjects extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		public clearBDObjects() { }
		public void action() { }
	}	
	
	/* WRITE LOG FILE */
	class writeLogFile extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		BufferedWriter fileWriter;

		public writeLogFile() { }

		public void action() {
			System.out.println("Agent " + getLocalName() +" writing log file ...");

			Calendar ts = Calendar.getInstance();
			String timestamp = ""+ts.get(Calendar.DAY_OF_MONTH)+"/"+(ts.get(Calendar.MONTH)+1)+"/"+ts.get(Calendar.YEAR)+" at "+ts.get(Calendar.HOUR)+":"+ts.get(Calendar.MINUTE)+":"+ts.get(Calendar.MILLISECOND);

			try {
				fileWriter = new BufferedWriter(new FileWriter(logfile));

				fileWriter.write("LOG FILE FOR MAS ANNOTATION SYSTEM");
				fileWriter.newLine();
				fileWriter.write("----------------------------------");
				fileWriter.newLine();
				fileWriter.write("Date and time: "+timestamp);
				fileWriter.newLine();
				fileWriter.write("Data file: "+datafilename);
				fileWriter.newLine();
				fileWriter.write("SNPs in file:"+rscount);
				fileWriter.newLine();
				fileWriter.write("SNPs annotated:"+annrscount);
				fileWriter.newLine();
				fileWriter.write("Execution time: "+exectime+" seconds");
				fileWriter.newLine();
				fileWriter.newLine();
				fileWriter.write("DATA PROVENANCE");
				fileWriter.newLine();
				fileWriter.write("---------------");
				fileWriter.newLine();
				fileWriter.write("Attribute \t Source \t Version");
				fileWriter.newLine();

				if(getAnalysistype()==1){
					Set<String> set;
					BioDataLite bd = new BioDataLite();
					bd.setBasicProvenance();
					set = bd.provenance_attrib.keySet();
					Iterator<String> itr = set.iterator();
					while (itr.hasNext()) {
						String attribute = itr.next();
						fileWriter.write(attribute+"\t"+bd.provenance_attrib.get(attribute)+"\t"+bd.provenance_version.get(bd.provenance_attrib.get(attribute)));
						fileWriter.newLine();
					}
				}

				if(getAnalysistype()==3){
					Set<String> set;
					BioDataLite bd = new BioDataLite();
					bd.setCompleteProvenance();
					set = bd.provenance_attrib.keySet();
					Iterator<String> itr = set.iterator();
					while (itr.hasNext()) {
						String attribute = itr.next();
						fileWriter.write(attribute+"\t"+bd.provenance_attrib.get(attribute)+"\t"+bd.provenance_version.get(bd.provenance_attrib.get(attribute)));
						fileWriter.newLine();
					}
				}
				
				if(getAnalysistype()==2){
					Set<String> set;
					BioData bd = new BioData();
					bd.setBasicProvenance();
					set = bd.provenance_attrib.keySet();
					Iterator<String> itr = set.iterator();
					while (itr.hasNext()) {
						String attribute = itr.next();
						fileWriter.write(attribute+"\t"+bd.provenance_attrib.get(attribute)+"\t"+bd.provenance_version.get(bd.provenance_attrib.get(attribute)));
						fileWriter.newLine();
					}
				}

				if(getAnalysistype()==4){
					Set<String> set;
					BioData bd = new BioData();
					bd.setCompleteProvenance();
					set = bd.provenance_attrib.keySet();
					Iterator<String> itr = set.iterator();
					while (itr.hasNext()) {
						String attribute = itr.next();
						fileWriter.write(attribute+"\t"+bd.provenance_attrib.get(attribute)+"\t"+bd.provenance_version.get(bd.provenance_attrib.get(attribute)));
						fileWriter.newLine();
					}
				}
				
				fileWriter.close();
			} catch (IOException e) { e.printStackTrace(); }
		}
	}


	/* SHUTDOWN PLATAFORM */
	class ShutDownPlatform extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		public void action() {
			Action actExpr = new Action();
			actExpr.setActor(getAMS());
			actExpr.setAction(new ShutdownPlatform());

			System.out.println("Agent " + getLocalName() +" shutting down the agent platform.");

			SLCodec codec = new SLCodec();
			getContentManager().registerOntology(JADEManagementOntology.getInstance());
			getContentManager().registerLanguage(codec, FIPANames.ContentLanguage.FIPA_SL0);

			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.addReceiver(getAMS());
			request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
			request.setOntology(JADEManagementOntology.NAME);

			try {      
				getContentManager().fillContent(request, actExpr);
				send(request);
			} catch (ContentException ce) { ce.printStackTrace(); }
		}
	}
	
	
	
	/* GENERIC FUNCTIONS */
	
	/* ENRICHMENT TEST FOR REMOTE ANNOTATION */
	public void EnrichmentTestRemote(String column, Map<String, MutableInt> miniMap, Map<String,String> completeMap, int sampleSize){
		Iterator<Entry<String, MutableInt>> itrE1 = miniMap.entrySet().iterator();
		while(itrE1.hasNext()){

			Iterator<Entry<String, String>> itrE2 = completeMap.entrySet().iterator();
			Map.Entry<String, MutableInt> entry1 = itrE1.next();
			while(itrE2.hasNext()){
				Map.Entry<String, String> entry2 = itrE2.next();
				if(entry1.getKey().equalsIgnoreCase(entry2.getKey()) && !entry1.getKey().contains("null")){
					String countCompleteMap = completeMap.get("COUNT");
					ContingencyTable2x2 Table = new ContingencyTable2x2(Integer.valueOf(entry1.getValue().getGeneCount()), sampleSize-Integer.valueOf(entry1.getValue().getGeneCount()), Integer.valueOf(entry2.getValue()), Integer.valueOf(countCompleteMap)-Integer.valueOf(entry2.getValue()));  
					FishersExactTest FisherTest = new FishersExactTest(Table);

					try {
						efileWriter.write(column+"\t"+entry1.getKey() +"\t"+FisherTest.getOneTailedSP() +"\t"+ entry2.getValue()+ "\t"+ entry1.getValue().toString()+"\n");
					} catch (IOException e) { e.printStackTrace(); }

					break;
				}
			}
		}
	}
	
	/* ENRICHMENT TEST FOR LOCAL ANNOTATION */
	public void EnrichmentTestLocal(Map<String, String> miniMap, Map<String,String> completeMap, int sampleSize){
		Iterator<Entry<String, String>> itrE1 = miniMap.entrySet().iterator();
		while(itrE1.hasNext()){
			Iterator<Entry<String, String>> itrE2 = completeMap.entrySet().iterator();
			Map.Entry<String, String> entry1 = itrE1.next();
			while(itrE2.hasNext()){
				Map.Entry<String, String> entry2 = itrE2.next();
				if(entry2.getKey().equalsIgnoreCase(entry1.getKey())){
					String countCompleteMap = completeMap.get("COUNT");
					ContingencyTable2x2 Table = new ContingencyTable2x2(Integer.valueOf(entry1.getValue()), sampleSize-Integer.valueOf(entry1.getValue()), Integer.valueOf(entry2.getValue()), Integer.valueOf(countCompleteMap)-Integer.valueOf(entry2.getValue()));  
					FishersExactTest FisherTest = new FishersExactTest(Table);
					
					try {
						efileWriter.write("Term - "+ entry1.getKey() +"\tTailedP = " + FisherTest.getOneTailedSP()+"\n")	;
					} catch (IOException e) { e.printStackTrace(); }
					
					break;
				}
			}
		}
		
		try {
			efileWriter.write("\n");
		} catch (IOException e) { e.printStackTrace(); }
	}


	
	/* DEPRECATED BEHAVIUORS */	
	
	class writeAnnotatedDB2 extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		BufferedWriter fileWriter;
		PreparedStatement st;
		PreparedStatement sti;
		String queryi;
		ResultSet rs;
		ResultSet rsi;
		int searchid;
		
		public writeAnnotatedDB2() { }

		public void action() {
			System.out.println("Agent " + getLocalName() +" retrieving annotation from database...");
			
			try {
				fileWriter = new BufferedWriter(new FileWriter(datapath+annotateddatafilename));
				connection = myconnection.mysqlConnect();
				Iterator<Integer> itr = searchidlist.iterator();

				if(getAnalysistype()==1){
					fileWriter.write("(1)Polymorphism Id \t (2)Polymorphism Type \t (3)Gene Symbol \t (4)Gene ID \t (5)Transcript Region \t (6)Nucleotide Numbering coding DNA \t (7)Chromosome \t (8)Chromosome Position \t (9)Ancestral Allele \t (10)Orientation \t (11)Assembly Build Version \t (12)Assembly Coord Start \t (13)Assembly Coord End \t  (14)mRNA accession \t (15)mRNA version \t (16)Alleles \t (17)Frequency");
					fileWriter.newLine();

					while(itr.hasNext()) {
					    int id = (Integer)itr.next();
					    String query = "SELECT * FROM snp,gene WHERE snp.fk_searchid=? AND gene.genepk=snp.fk_genepk";
		 
						st = connection.prepareStatement(query);
						st.setInt(1,id);
						rs = st.executeQuery();

						while (rs.next())  {
							fileWriter.write("rs"+rs.getString(2)+"\t"+rs.getString(3)+"\t"+rs.getString(22)+"\t"+rs.getString(23)+"\t"+rs.getString(4)+"\t"+rs.getString(7)+"\t"+rs.getString(8)+"\t"+rs.getString(7)+"\t"+rs.getString(14)+"\t"+rs.getString(15)+"\t"+rs.getString(10)+"\t"+rs.getString(11)+"\t"+rs.getString(12)+"\t"+rs.getString(16)+"\t"+rs.getString(17)+"\t"+rs.getString(5)+"\t"+rs.getString(18));
						}

						st.close();
					}
				}

				if(getAnalysistype()==3){
					fileWriter.write("PolymorphismId(1)\tPolymorphismType(2)\tGeneSymbol(3)\tGeneId(4)\tTranscriptRegion(5)\tNucleotideNumberingCodingDNA(6)\tChromosome(7)\tChromosomePosition(8)\tAncestralAllele(9)\tOrientation(10)\tAssemblyBuildVersion(11)\tAssemblyCoordStart(12)\tAssemblyCoordEnd(13)\tmRNAaccession(14)\tmRNAversion(15)\tAlleles(16)\tFrequency(17)\tStrand(18)\tRefUCSC(19)\tObservedUCSC(20)\tPolymorphismClass(21)\tFunctionalClass(22)\tPathways(23)\tDrugs(24)\tDisease(25)\tRelatedGenes(26)\tGOMolecularFunction(27)\tGOCellularComponent(28)\tGOBiologicalProcess(29)\tCytoloc(30)\tGeneStatus(31)\tGeneMapMethods(32)\tDisorders(33)\tMIMids(34)\tInheritance(35)\tPhenoMapMethods(36)\tComments(37)\tHgncId(38)\tGeneSymbol(39)\tGeneName(40)\tGeneSynonyms(41)\tLocusType(42)\tLocusGroup(43)\tGeneFamilyTag(44)\tGeneFamily(45)\tPubmed(46)\tReported_Genes(47)\tStrongest_SNP_Risk_Allele(48)\tContext(49)\tP-Value(50)\tDisease_Trait(51)\tSample_and_Population(52)");
					fileWriter.newLine();
			
					while(itr.hasNext()) {
					    int id = (Integer)itr.next();
						String strsql = "select snp.snpid, " +
										"snp.kind, " +
										"gene.gene_symbol, " +
										"gene.geneid, " +
										"snp.subkind, " +
										"snp.coordRelGene, " +
										"snp.chromosome, " +
										"snp.coordRefSeq, " +
										"snp.ancestral_allele, " +
										"snp.orientation, " +
										"snp.assm_build_version, " +
										"snp.assm_coord_start, " +
										"snp.assm_coord_end, " +
										"snp.mrnaAcc, " +
										"snp.mrnaVer, " +
										"snp.referenceValue, " +
										"snp.freq, " +
										"ucsc.strand, " +
										"ucsc.refUCSC, " +
										"ucsc.observed, " +
										"ucsc.class, " +
										"ucsc.func, " +
										"pharmGKB.pathway, " +
										"pharmGKB.drugs, " +
										"pharmGKB.disease, " +
										"pharmGKB.geneCross, " +
										"geneOntology.molFunction, " +
										"geneOntology.celComp, " +
										"geneOntology.bioProcess, " +
										"omim.cytoLoc, " +
										"omim.geneStatus, " +
										"omim.methods, " +
										"omim.disorder, " +
										"omim.mimId, " +
										"omim.inheritance, " +
										"omim.phenmap, " +
										"omim.comments, " +
										"hugoDB.hgncId, " +
										"hugoDB.approvedSymbol, " +
										"hugoDB.approvedName, " +
										"hugoDB.synonyms, " +
										"hugoDB.locusType, " +
										"hugoDB.locusGroup, " +
										"hugoDB.geneFamilyTag, " +
										"hugoDB.geneFamilyDesc, " +
										"gwascatalog.pubmedid, " +
										"gwascatalog.reported_genes, " +
										"gwascatalog.strongest_snp_risk_allele, " +
										"gwascatalog.context, " +
										"gwascatalog.pvalue, " +
										"gwascatalog.disease_trait, " +
										"gwascatalog.initial_sample_size " +
										"FROM snp " +
										"LEFT JOIN gene ON snp.fk_genepk = gene.genepk " +
										"LEFT JOIN ucsc ON snp.snpid = ucsc.polID " +
										"LEFT JOIN pharmGKB ON gene.gene_symbol = pharmGKB.geneSymbol " +
										"LEFT JOIN geneOntology ON gene.gene_symbol = geneOntology.gp_symbol " +
										"LEFT JOIN omim ON gene.gene_symbol = omim.geneSymbols " +
										"LEFT JOIN hugoDB ON gene.gene_symbol = hugoDB.approvedSymbol " +
										"LEFT JOIN gwascatalog ON snp.snpid = gwascatalog.snps " +
										"WHERE snp.fk_searchid=?;";

						st = connection.prepareStatement(strsql);
						st.setInt(1,id);
						rs = st.executeQuery();

						while (rs.next())  {
					    	 fileWriter.write("rs"+rs.getString(1)+
							    			  "\t"+rs.getString(2)+
							    			  "\t"+rs.getString(3)+
							    			  "\t"+rs.getString(4)+
							    			  "\t"+rs.getString(5)+
							    			  "\t"+rs.getString(6)+
							    			  "\t"+rs.getString(7)+
							    			  "\t"+rs.getString(8)+
							    			  "\t"+rs.getString(9)+
							    			  "\t"+rs.getString(10)+
							    			  "\t"+rs.getString(11)+
							    			  "\t"+rs.getString(12)+
							    			  "\t"+rs.getString(13)+
							    			  "\t"+rs.getString(14)+
							    			  "\t"+rs.getString(15)+
							    			  "\t"+rs.getString(16)+
							    			  "\t"+rs.getString(17)+
							    			  "\t"+rs.getString(18)+
							    			  "\t"+rs.getString(19)+
							    			  "\t"+rs.getString(20)+
							    			  "\t"+rs.getString(21)+
							    			  "\t"+rs.getString(22)+
							    			  "\t"+rs.getString(23)+
							    			  "\t"+rs.getString(24)+
							    			  "\t"+rs.getString(25)+
							    			  "\t"+rs.getString(26)+
							    			  "\t"+rs.getString(27)+
							    			  "\t"+rs.getString(28)+
			 				    			  "\t"+rs.getString(29)+
							    			  "\t"+rs.getString(30)+
							    			  "\t"+rs.getString(31)+				    			 
							    			  "\t"+rs.getString(32)+
							    			  "\t"+rs.getString(33)+
							    			  "\t"+rs.getString(34)+
							    			  "\tnull"+
							    			  "\tnull"+
							    			  "\t"+rs.getString(37)+
							    			  "\t"+rs.getString(38)+
							    			  "\t"+rs.getString(39)+
							    			  "\t"+rs.getString(40)+
							    			  "\t"+rs.getString(41)+				    			 
							    			  "\t"+rs.getString(42)+
							    			  "\t"+rs.getString(43)+
							    			  "\t"+rs.getString(44)+
							    			  "\t"+rs.getString(45)+
							    			  "\t"+rs.getString(46)+
							    			  "\t"+rs.getString(47)+
							    			  "\t"+rs.getString(48)+
							    			  "\t"+rs.getString(49)+
							    			  "\t"+rs.getString(50)+
							    			  "\t"+rs.getString(51)+				    			 
							    			  "\t"+rs.getString(52)+"\n");
						}

						st.close();
					}
				}
				
				fileWriter.close();
			} catch (IOException e)  { e.printStackTrace(); }
			  catch (SQLException e) { e.printStackTrace(); }
		}
		
		public int onEnd() {
			System.out.println("Agent " + getLocalName() +" finished writing annotated report on "+datapath+annotateddatafilename);
			myconnection.mysqlDisconnect();
			end = System.currentTimeMillis();
			exectime = (end - start)/1000;
			System.out.println("TOTAL ANNOTATION TIME: "+exectime+"s");

			return 0;
		}
	}

	/* WRITE SUMMARY FROM ANNOTATION DATABASE */
	class writeSummaryDB extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		BufferedReader inputStream;
		BufferedWriter fileWriter;
		int transRegSize = 0;
		int functionSize = 0;
		int pgkbPathSize = 0;
		int pgkbDiseaseSize = 0;
		int pgkbDrugSize = 0;
		int omimDisorderSize = 0;
		int omimCytoLocSize = 0;
		int goBioPSize = 0;
		int goCelCSize = 0;
		int goMolFSize = 0;
		int gfTagSize = 0;
		
		public writeSummaryDB(){ }
		
		public void action(){
			System.out.println("Agent " + getLocalName() +" writing annotation summary at "+ sumfile2 + "...");
			
			Map<String, String> geneCount = new HashMap<String, String>();
			Map<String, String> fxnCount = new HashMap<String, String>();
			Map<String, String> chrCount = new HashMap<String, String>();
			Map<String, String> functionCount = new HashMap<String, String>();
			Map<String, String> pathwayCount = new HashMap<String, String>();
			Map<String, String> drugCount = new HashMap<String, String>();
			Map<String, String> diseaseCount = new HashMap<String, String>();
			Map<String, String> goMolFunCount = new HashMap<String, String>();
			Map<String, String> goCelCompCount = new HashMap<String, String>();
			Map<String, String> goBioProCount = new HashMap<String, String>();
			Map<String, String> cytoLocCount = new HashMap<String, String>();
			Map<String, String> disorderCount = new HashMap<String, String>();
			Map<String, String> locTypeCount = new HashMap<String, String>();
			Map<String, String> locGroupCount = new HashMap<String, String>();
			Map<String, String> gfTagCount = new HashMap<String, String>();
			
			try {
				connection = myconnection.mysqlConnect();
	
				Statement st = connection.createStatement();
				ResultSet rs = st.executeQuery("SELECT gene_symbol, count(*) FROM gene GROUP BY gene_symbol;");
			    
				while (rs.next()) {
					String gene = rs.getString(1);
					String genCount = rs.getString(2);
					geneCount.put(gene, genCount);
				}
	
				st = connection.createStatement();
			    rs = st.executeQuery("SELECT subkind, count(*) FROM snp GROUP BY subkind;");
	
				while (rs.next()) {
					String fxnClass = rs.getString(1);
					String fxnClassCount = rs.getString(2);
					fxnCount.put(fxnClass, fxnClassCount);
				}
	
				st = connection.createStatement();
			    rs = st.executeQuery("SELECT chromosome, count(*) FROM snp GROUP BY chromosome;");
			    
				while (rs.next()) {
					String chr = rs.getString(1);
					String chrCCount = rs.getString(2);
					chrCount.put(chr, chrCCount);
				}
	
				if(getAnalysistype()==3){
					st = connection.createStatement();
					rs = st.executeQuery("SELECT func, count(*) FROM ucsc GROUP BY func;");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						functionCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT pgkb.pathway.name, Count(DISTINCT pgkb.pathway.name) " +
							"FROM " +
							"pharmGKB INNER JOIN pgkb.pathway " +
							"ON FIND_IN_SET(pgkb.pathway.name, REPLACE(pharmGKB.pathway, ';', ','))>0 " +
							"GROUP BY " + 
							"pgkb.pathway.name; ");
					
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						pathwayCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT pgkb.drug.name, Count(DISTINCT pgkb.drug.name) " +
							"FROM " +
							"pharmGKB INNER JOIN pgkb.drug " +
							"ON FIND_IN_SET(pgkb.drug.name, REPLACE(pharmGKB.drugs, ';', ','))>0 " +
							"GROUP BY " + 
							"pgkb.drug.name; ");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						drugCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT pgkb.disease.name, Count(DISTINCT pgkb.disease.name) " +
							"FROM " +
							"pharmGKB INNER JOIN pgkb.disease " +
							"ON FIND_IN_SET(pgkb.disease.name, REPLACE(pharmGKB.disease, ';', ','))>0 " +
							"GROUP BY " + 
							"pgkb.disease.name; ");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						diseaseCount.put(key, count);
					}
					
					st = connection.createStatement();
					rs = st.executeQuery("SELECT GO.term.name, Count(*) " +
							"FROM " +
							"geneOntology INNER JOIN GO.term " +
							"ON FIND_IN_SET(GO.term.name, REPLACE(geneOntology.molFunction, ';', ','))>0 " +
							"WHERE GO.term.term_type = 'molecular_function' " + 
							"GROUP BY " + 
							"GO.term.name; ");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						goMolFunCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT GO.term.name, Count(*) " +
							"FROM " +
							"geneOntology INNER JOIN GO.term " +
							"ON FIND_IN_SET(GO.term.name, REPLACE(geneOntology.celComp, ';', ','))>0 " +
							"WHERE GO.term.term_type = 'cellular_component' " + 
							"GROUP BY " + 
							"GO.term.name; ");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						goCelCompCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT GO.term.name, Count(*) " +
							"FROM " +
							"geneOntology INNER JOIN GO.term " +
							"ON FIND_IN_SET(GO.term.name, REPLACE(geneOntology.bioProcess, ';', ','))>0 " +
							"WHERE GO.term.term_type = 'biological_process' " + 
							"GROUP BY " + 
							"GO.term.name; ");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						goBioProCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT cytoLoc, count(*) FROM omim GROUP BY cytoLoc;");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						cytoLocCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT OMIM.genemap.disorder1, Count(*) " +
							"FROM " +
							"omim INNER JOIN OMIM.genemap " +
							"ON FIND_IN_SET(OMIM.genemap.disorder1, REPLACE(omim.disorder, ';', ','))>0 " +
							"GROUP BY " + 
							"OMIM.genemap.disorder1;");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						disorderCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT locusType, count(*) FROM hugoDB GROUP BY locusType;");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						locTypeCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT locusGroup, count(*) FROM hugoDB GROUP BY locusGroup;");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						locGroupCount.put(key, count);
					}
		
					st = connection.createStatement();
					rs = st.executeQuery("SELECT geneFamilyTag, count(*) FROM hugoDB GROUP BY geneFamilyTag;");
				    
					while (rs.next()) {
						String key = rs.getString(1);
						String count = rs.getString(2);
						gfTagCount.put(key, count);
					}
				}
				
				st.close();
				rs.close();
				myconnection.mysqlDisconnect();
			} catch (SQLException e){ e.printStackTrace(); }

			try{
				fileWriter = new BufferedWriter(new FileWriter(sumfile2));
				Iterator<Entry<String, String>> itr = geneCount.entrySet().iterator(); 

				while(itr.hasNext()){
					Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					fileWriter.write("Gene Symbol dbSNP\t" +entry.getKey()+"\t"+entry.getValue()+"\n");	
				}
				
				itr = fxnCount.entrySet().iterator(); 
				
				while(itr.hasNext()){
					Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					fileWriter.write("Transcript Region dbSNP\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					transRegSize = transRegSize + Integer.valueOf(entry.getValue());
				}
				
				itr = chrCount.entrySet().iterator(); 
				
				while(itr.hasNext()){
					Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					fileWriter.write("Chromosome dbSNP\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
				}
				
				if(getAnalysistype()==3){
					itr = functionCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Transcript region UCSC\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					}
				
					itr = pathwayCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Pathway PharmGKB\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					    pgkbPathSize = pgkbPathSize + Integer.valueOf(entry.getValue());
					}

					itr = drugCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Drugs PharmGKB\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					    pgkbDrugSize = pgkbDrugSize + Integer.valueOf(entry.getValue());
					}

					itr = diseaseCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Disease PharmGKB\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					    pgkbDiseaseSize = pgkbDiseaseSize + Integer.valueOf(entry.getValue());
					}

					itr = goMolFunCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Molecular Function GO\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					    goMolFSize = goMolFSize + Integer.valueOf(entry.getValue());
					}

					itr = goCelCompCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Cellular Component GO\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					    goCelCSize = goCelCSize + Integer.valueOf(entry.getValue());
					}

					itr = goBioProCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Biological Process PharmGKB\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					    goBioPSize = goBioPSize + Integer.valueOf(entry.getValue());
					}

					itr = cytoLocCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("CytoLocation OMIM\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					    omimCytoLocSize = omimCytoLocSize + Integer.valueOf(entry.getValue());
					}

					itr = disorderCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Disorders OMIM\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					    omimDisorderSize = omimDisorderSize + Integer.valueOf(entry.getValue());
					}

					itr = locTypeCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Locus Type HGNC\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					}

					itr = locGroupCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Locus Group HGNC\t"+entry.getKey()+"\t"+entry.getValue()+"\n");	
					}

					itr = gfTagCount.entrySet().iterator();
					
					while(itr.hasNext()){
						Map.Entry<String, String> entry = (Entry<String, String>) itr.next();
					    fileWriter.write("Gene Family HGNC\t"+entry.getKey()+"\t"+entry.getValue()+"\n");
					    gfTagSize = gfTagSize + Integer.valueOf(entry.getValue());
					}
					
					efileWriter = new BufferedWriter(new FileWriter(enrichfile));
					System.out.println("ENRICHMENT TEST LOCAL");
					
					BioEnrichment hgncCount = new BioEnrichment();
					hgncCount.setHgncGeneFamilyCount();
					System.out.println("Enrichment Test GeneFamily - Sample Matrix Size = "+ gfTagCount.size());
					efileWriter.write("HGNC Gene Family Enrichment Test\n");
					
					BioEnrichment edisorderCount = new BioEnrichment();
					edisorderCount.setomimDisorderCount();
					System.out.println("Enrichment Test OmimDisorder - Sample Matrix Size = "+ disorderCount.size());
					efileWriter.write("OMIM Disorder Enrichment Test\n");
					
					BioEnrichment ecytoLocCount = new BioEnrichment();
					ecytoLocCount.setomimCytoLocCount();
					System.out.println("Enrichment Test CytoLoc - Sample Matrix Size = "+ cytoLocCount.size());
					efileWriter.write("OMIM CytoLoc Enrichment Test\n");

					BioEnrichment etransRegCount = new BioEnrichment();
					etransRegCount.setdbsnpTransRegCount();
					System.out.println("Enrichment Test TranscRegion - Sample Matrix Size = "+ fxnCount.size());
					efileWriter.write("dbSNP Transcript Region Enrichment Test\n");
					
					BioEnrichment egoMolFunCount = new BioEnrichment();
					egoMolFunCount.setgoMolFunctionCount();
					System.out.println("Enrichment Test GOMolFunction - Sample Matrix Size = "+ goMolFunCount.size());
					efileWriter.write("GeneOntology Molecular Function Enrichment Test\n");
					
					BioEnrichment egoBioPCount = new BioEnrichment();
					egoBioPCount.setgoBioProcessCount1();
					egoBioPCount.setgoBioProcessCount2();
					egoBioPCount.setgoBioProcessCount3();
					System.out.println("Enrichment Test GOBioProcess - Sample Matrix Size = "+ goBioProCount.size());
					efileWriter.write("GeneOntology Biological Process Enrichment Test\n");
					
					BioEnrichment egoCelCompCount = new BioEnrichment();
					egoCelCompCount.setgoCelCompCount();
					System.out.println("Enrichment Test GOCelComp - Sample Matrix Size = "+ goCelCompCount.size());
					efileWriter.write("GeneOntology Cellular Component Enrichment Test\n");
					
					BioEnrichment2 edrugCount = new BioEnrichment2();
					edrugCount.setpgkbDrugCount();
					System.out.println("Enrichment Test PGKB Drug - Sample Matrix Size = "+ drugCount.size());
					efileWriter.write("PharmGKB Drugs Enrichment Test\n");
					
					BioEnrichment2 epathwayCount = new BioEnrichment2();
					epathwayCount.setpgkbPathwayCount();
					System.out.println("Enrichment Test PGKB Pathway - Sample Matrix Size = "+ pathwayCount.size());
					efileWriter.write("PharnGKB Pathways Enrichment Test\n");
					
					BioEnrichment2 ediseaseCount = new BioEnrichment2();
					ediseaseCount.setpgkbDiseaseCount();
					System.out.println("Enrichment Test PGKB Disease - Sample Matrix Size = "+ diseaseCount.size());
					efileWriter.write("PharmGKB Diseases Test\n");
				}
	
				fileWriter.close();
				efileWriter.close();
			} catch (IOException e) { e.printStackTrace(); }	
		}
	}

	
	
	
	/* GETS AND SETS LIST */
	public String getServicetype() {
		return servicetype;
	}

	public void setServicetype(String s) {
		this.servicetype = s;
	}

    public String getInfoType() {
		return infotype;
	}

	public void setInfoType(String i) {
		this.infotype = i;
	}

	public String getDatafilename() {
		return datafilename;
	}

	public void setDatafilename(String datafilename) {
		this.datafilename = datafilename;
	}

	public int getAnalysistype() {
		return analysistype;
	}

	public void setAnalysistype(int analysistype) {
		this.analysistype = analysistype;
	}

	public String getAnnotateddatafilename() {
		return annotateddatafilename;
	}

	public void setAnnotateddatafilename(String annotateddatafilename) {
		this.annotateddatafilename = annotateddatafilename;
	}

	public void setLogfileName(String lf){
		this.logfile    = this.datapath+lf;
	}
	
	public String getLogfileName(){
		return this.logfile;
	}
	
	public void setSummaryfileName(String sf){
		this.sumfile    = this.datapath+sf;
	}
	
	public String getSummaryfileName(){
		return this.sumfile;
	}

	public void setEnrichfileName(String ef){
		this.enrichfile    = this.datapath+ef;
	}
	
	public String getEnrichfileName(){
		return this.enrichfile;
	}

	public void setSummaryfileDBName(String sf){
		this.sumfile2    = this.datapath+sf;
	}
	
	public String getSummaryDBfileName(){
		return this.sumfile2;
	}

	public int getPquery() {
		return pquery;
	}

	public void setPquery(int pquery) {
		this.pquery = pquery;
	}
}