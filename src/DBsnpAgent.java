import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import BioData_Models.Polymorphism;

public class DBsnpAgent extends DBagent {
	private static final long serialVersionUID = 1L;

	/* Attributes */
	BioData snpdata;
	int input_length;
	int[] input;

	/* Constructor */
	public DBsnpAgent() {
		super();
		this.setDBname("dbsnp");
   	 	this.setInformation("snp");
   	 	this.snpdata = new BioData("snp");
	}
	
	/* Agent setup */
	protected void setup() {
		System.out.println("Agent " + getLocalName() + " started.");
		this.register();
		addBehaviour(new waitRequest());
	}
	
	/* Agent shutdown */
	
	
	
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
					BioData msgcontent = (BioData)msg.getContentObject();
					System.out.println("Agent " + getLocalName() +" executing request...");
					addBehaviour(new dbSNPAction(msgcontent.getSnpIdList(),sender));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	} 

	/* Agent action: access DB -> access DB remote; send reply  */
	class dbSNPAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public AID partneragent;
		public int[] snpidlist;
		
		public dbSNPAction(int[] sl, AID pa){
			partneragent = pa;
			snpidlist = sl;
			
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessDBsnpEutils(snpidlist), STATE_A);
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
		private ACLMessage msg;

		public SendReply(AID p) {
			msgreceiver     = p;
			msgperformative = "INFORM";
			snpdata.setSearchid(getAnnsearchid());
		}
		
		public void action() {
			System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
			msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(msgreceiver);
			msg.setLanguage("English");

			try {
				msg.setContentObject(snpdata);
			} catch (IOException e) { e.printStackTrace(); }
			myAgent.send(msg);
		}
	}
	
	/* Agent action: parallel access to DB -> remote Access DB  */	
	class accessDBsnpEutils extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private int[] snpidlist;

		public accessDBsnpEutils (int[] sl) {
			super(WHEN_ALL);
			this.snpidlist = sl;
		}
		
		public void onStart(){
			System.out.println("Threads ativas:"+Thread.getAllStackTraces().size());
			
			for (int i = 0; i < snpidlist.length; i++) {
				this.addSubBehaviour(new remoteAccessDBsnpEutils(snpidlist[i]));
			}
		}
		
		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}		
	
	/* Agent action: Query DB  */
	class remoteAccessDBsnpEutils extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private int snpid;
		UrlFilter urlfilter;
		private Polymorphism snpresult;

		public remoteAccessDBsnpEutils(int l) {
			snpid = l;
	   	 	this.urlfilter = new UrlFilter(snpid);
	   	 	this.snpresult = snpdata.createPolymorphismInstance();
		}

		public void action() {
			urlfilter.readFLT();
			urlfilter.readXML();

			for (int x = 0; x < urlfilter.getPolTableSize(); x++) {
				if(urlfilter.polTable[x] == null){
					urlfilter.polTable[x] = "null";
				}
			}
			
			snpresult.setPolymorphismCode(Integer.toString(snpid));
			snpresult.setKind(urlfilter.polTable[1]);
			snpresult.setSubKind(urlfilter.polTable[2]);
			snpresult.setReferenceValue(urlfilter.polTable[3]);
			snpresult.setCoordRelGene(urlfilter.polTable[4]);
			snpresult.setChromosome(urlfilter.polTable[5]);
			snpresult.setCoordRefSeq(urlfilter.polTable[6]);
			snpresult.setAssm_build_version(urlfilter.polTable[7]);
			snpresult.setAssm_coord_start(urlfilter.polTable[8]);
			snpresult.setAssm_coord_end(urlfilter.polTable[9]);
			snpresult.setGenesymbol(urlfilter.polTable[13]);
			snpresult.setValue(urlfilter.polTable[14]);
			snpresult.setAncestralAllele(urlfilter.polTable[15]);
			snpresult.setOrientation(urlfilter.polTable[16]);
			snpresult.setMrnaAcc(urlfilter.polTable[17]);
			snpresult.setMrnaVer(urlfilter.polTable[18]);
			snpresult.setFreq(urlfilter.polTable[19]);
			snpresult.setReferenceAllele(urlfilter.polTable[20]);
			
			if(urlfilter.polTable[12] != "NA"){
				snpresult.setGenesymbol(urlfilter.polTable[13]);
				snpresult.setGeneid(urlfilter.polTable[12]);
			} else {
				snpresult.setGenesymbol("null");
				snpresult.setGeneid("null");
			}
			
			synchronized(snpdata){
				snpdata.setPolymorphismList(snpresult);
			}
		}
	}


	/* LIST OF QUERIES */

	/* Agent action: Query DBSNP FLAT FILES  */
	class UrlFilter {
		int id;
		String[] polTable;
		int firstRes = 0;
		
		public UrlFilter(int id){
			polTable = new String[21];
			setId(id);
		}
		
		public int getPolTableSize(){
			return polTable.length;
		}
		
		public void readFLT() {
			try {
				URL url = new URL(makeURL(id, "FLT"));
				BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
				String line;

				do {
					line = reader.readLine();

					if (line.startsWith("SNP")) {
						String alleles = null;
						int inicio = 0;
						int fim = 0;
						
						inicio = line.indexOf("alleles=") + 8;
						alleles = line.substring(inicio);
						fim = alleles.indexOf(" | ");
						alleles = alleles.substring(0, fim);
						
						polTable[3] = alleles;
					} else if (line.startsWith("CTG")) {
						if(firstRes==0){
							String[] aux = new String[17];
							aux = line.split(" | ");
		
							String assembly = aux[2];
							String chr = aux[4];
							String chr_pos = aux[6];
							String ctg_start = aux[10];
		
							int pos;
		
							pos = assembly.indexOf('=');
							assembly = assembly.substring(pos + 1);
		
							polTable[7] = assembly;
		
							pos = chr.indexOf('=');
							chr = chr.substring(pos + 1);
		
							polTable[5] = chr;
		
							pos = chr_pos.indexOf('=');
							chr_pos = chr_pos.substring(pos + 1);
							
							pos = ctg_start.indexOf('=');
							ctg_start = ctg_start.substring(pos + 1);
							
							polTable[6] = chr_pos;
							polTable[8] = ctg_start;
							polTable[9] = ctg_start;
							firstRes = 1;
						}
					} else if (line.startsWith("LOC")) {
						String[] aux = new String[6];
						aux = line.split(" | ");

						String gene_name = aux[2];
						String locus_id = aux[4];
						String fxn_class = aux[6];
						
						int pos;

						pos = locus_id.indexOf('=');
						locus_id = locus_id.substring(pos + 1);

						polTable[12] = locus_id;

						pos = fxn_class.indexOf('=');
						fxn_class = fxn_class.substring(pos + 1);

						polTable[2] = fxn_class;
						
						polTable[13] = gene_name;
					}
				} while (reader.ready());
				reader.close();
			} catch (MalformedURLException e) {
				System.err.println("ERROR 1: ERROR READING FLT URL: MalformedURLException - readFLT()");
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("ERROR 2: IOException - readFLT()");
				e.printStackTrace();
			}
		}

		/* Agent action: Query DBSNP XML FILES  */
		public void readXML() {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			DocumentBuilder docBuilder;
			Element AssemblyTag;
			Element ComponentTag;
			Element MapLocTag;
			Element FxnSetTag;
			
			try {
				docBuilder = dbf.newDocumentBuilder();
				URL url = new URL(makeURL(id, "XML"));
				Document doc = docBuilder.parse(url.openStream());
				doc.getDocumentElement().normalize();

				Element rootTag = doc.getDocumentElement();

				Element rsTag = (Element) rootTag.getElementsByTagName("Rs").item(0);

				int i=0;
				Element hgvsTag;
				String coord_relative_gene;
				do {
					try{
						hgvsTag = (Element) rsTag.getElementsByTagName("hgvs").item(i);
						coord_relative_gene = hgvsTag.getTextContent();
					} catch(Exception e){ coord_relative_gene = null; }
					i++;
				}
				while(coord_relative_gene!=null && !coord_relative_gene.startsWith("NM_"));
				coord_relative_gene = process_coord_relative_gene(coord_relative_gene);
				
				try{
					polTable[1] = rsTag.getAttribute("snpClass");
				} catch(Exception e){ polTable[1] = null; }
				
				polTable[4] = coord_relative_gene;
				
				try{
					Element hetTag = (Element) rsTag.getElementsByTagName("Het").item(0);
					polTable[14] = hetTag.getAttribute("value");
				} catch(Exception e){ polTable[14] = null; }
				
				try{
					Element SequenceTag = (Element) rsTag.getElementsByTagName("Sequence").item(0);
					polTable[15] = SequenceTag.getAttribute("ancestralAllele");
					if(polTable[15].equals("")){
						polTable[15] = null;
					}
				} catch(Exception e){ polTable[15] = null; }
				
				try{
					AssemblyTag = (Element) rsTag.getElementsByTagName("Assembly").item(0);
					ComponentTag = (Element) AssemblyTag.getElementsByTagName("Component").item(0);
					
					polTable[16] = ComponentTag.getAttribute("orientation");	
				} catch(Exception e){ polTable[16] = null; }
				
				try{
					AssemblyTag = (Element) rsTag.getElementsByTagName("Assembly").item(0);
					ComponentTag = (Element) AssemblyTag.getElementsByTagName("Component").item(0);
					MapLocTag = (Element) ComponentTag.getElementsByTagName("MapLoc").item(0);
					FxnSetTag = (Element) MapLocTag.getElementsByTagName("FxnSet").item(0);

					polTable[17] = FxnSetTag.getAttribute("mrnaAcc");
				} catch (Exception e) { polTable[17] = null; }
				
				try{
					AssemblyTag = (Element) rsTag.getElementsByTagName("Assembly").item(0);
					ComponentTag = (Element) AssemblyTag.getElementsByTagName("Component").item(0);
					MapLocTag = (Element) ComponentTag.getElementsByTagName("MapLoc").item(0);
					FxnSetTag = (Element) MapLocTag.getElementsByTagName("FxnSet").item(0);
					
					polTable[18] = FxnSetTag.getAttribute("mrnaVer");
				} catch (Exception e) { polTable[18] = null; }
				
				try{
					Element FrequencyTag = (Element) rsTag.getElementsByTagName("Frequency").item(0);
					polTable[19] = FrequencyTag.getAttribute("freq");
				} catch (Exception e) { polTable[19] = null; }
				
				try{
					Element FrequencyTag = (Element) rsTag.getElementsByTagName("Frequency").item(0);
					polTable[20] = FrequencyTag.getAttribute("allele");
				} catch (Exception e){ polTable[20]=null; }

			} catch (ParserConfigurationException e) {
				System.err.println("ERROR 3: ParserConfigurationException - readXML()");
				e.printStackTrace();
			} catch (MalformedURLException e) {
				System.err.println("ERROR 4: ERROR READING XML URL: MalformedURLException - readXML()");
				e.printStackTrace();
			} catch (SAXException e) {
				System.err.println("ERROR 5: SAXException - readXML()");
				System.err.println("polTable[4] setting to <ERROR> ...");
				polTable[4] = "<ERROR>";
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("ERROR 6: IOException - readXML()");
				e.printStackTrace();
			}
		}
		
		
		/* GENERIC FUNCTIONS */
		
		/* PROCESS GENE COORDINATES */
		public String process_coord_relative_gene(String coord_relative_gene){
			if(coord_relative_gene!=null){
				String aux = coord_relative_gene.split(":")[1];
				aux = aux.substring(0, aux.length()-3);
				return aux;
			}
			return null;
		}

		/* SET ID */
		public void setId(int id) {
			this.id = id;
			polTable[0] = "" + this.id;
		}

		/* CREATE QUERY URL */
		public String makeURL(int id, String report) {
			return 
			"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=snp&id=" + id + "&report=" + report;
		}
	}
}

