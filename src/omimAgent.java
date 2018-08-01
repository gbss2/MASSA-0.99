import java.net.URL;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Set;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import BioData_Models.OMIM;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.MessageTemplate;

public class omimAgent extends DBagent{
	private static final long serialVersionUID = 1L;

	/* Attributes */	
	BioData omimdata;

	/* Constructor */	
	public omimAgent() {
		super();
		this.setDBname("omim");
		this.setInformation("omim");
		this.omimdata = new BioData("omim");
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

		public waitRequest() { MessageTemplate.MatchPerformative(ACLMessage.REQUEST); }
		
		public void action() {
			ACLMessage msg = myAgent.receive(simplerequest_template);
			if (msg != null) {
				System.out.println("Agent " + getLocalName() +" received a REQUEST message from agent "+msg.getSender().getName());
				try {
					sender = msg.getSender();
					BioData msgcontent = (BioData)msg.getContentObject();
					System.out.println("Agent " + getLocalName() +" executing request...");
					addBehaviour(new omimAction(sender,msgcontent));
				} catch (UnreadableException e) { e.printStackTrace(); }
			} else { block(); }
		}
	}

	/* Agent action: access DB -> access DB remote; send reply  */
	class omimAction extends FSMBehaviour {
		private static final long serialVersionUID = 1L;

		private static final String STATE_A = "A";
		private static final String STATE_B = "B";
		
		public omimAction(AID pa, BioData bd){
			System.out.println("Agent " + getLocalName() +" executing request...");
			this.registerFirstState(new accessOmimAPI(bd), STATE_A);
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
		private ACLMessage msg;

		public SendReply(AID p) {
			msgreceiver     = p;
			msgperformative = "INFORM";
		}
		
		public void action() {
			System.out.println("... sending "+msgperformative+" to agent: \""+msgreceiver.getName());
			msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(msgreceiver);
			msg.setLanguage("English");

			try { msg.setContentObject(omimdata); }
			catch (IOException e) { e.printStackTrace(); }

			myAgent.send(msg);
		}
	}
	
	/* Agent action: parallel access to DB -> remote Access DB  */
	class accessOmimAPI extends ParallelBehaviour {
		private static final long serialVersionUID = 1L;

		private BioData contentdata;

		public accessOmimAPI (BioData bd) {
			super(WHEN_ALL);
			this.contentdata = bd;
		}
		
		public void onStart(){
			Set<String> keyset;
			keyset = contentdata.snp_gene.keySet();
			Iterator<String> itr = keyset.iterator();
			
			while (itr.hasNext()) {
				String snp  = itr.next();
				String gene = contentdata.snp_gene.get(snp);
				this.addSubBehaviour(new remoteAccessOmimAPI(snp,gene));
			}
		}
		
		public int onEnd(){
			System.out.println("Agent " + getLocalName() +" finished.");
			return super.onEnd();
		}
	}
	
	/* Agent action: Query DB  */	
	class remoteAccessOmimAPI extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		private String snpid;
		private String genesymbol;
		private OMIM omimobject;
		UrlFilter urlfilter;

		public remoteAccessOmimAPI(String s, String g) {
			this.snpid = s;
			this.genesymbol = g;
	   	 	this.urlfilter = new UrlFilter(this.genesymbol);
	   	 	this.omimobject = omimdata.createOMIMInstance();
		}

		public void action() {
			omimobject = urlfilter.readXML();
			omimobject.setGenesymbol(genesymbol);
			omimobject.setPolymorphismCode(snpid);

			synchronized(omimdata){
				omimdata.setOMIMList(omimobject);
			}
		}
	}


	/* OMIM URL ACCESS */
	class UrlFilter {
		String geneName;
		BioData bioobjects;
		OMIM omimobject;
		
		public UrlFilter(String g){
			this.geneName = g;
			bioobjects = new BioData("omim");
			omimobject = bioobjects.createOMIMInstance();
		}

		public OMIM readXML() {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder docBuilder;

			try {
				docBuilder = dbf.newDocumentBuilder();
				URL url = new URL(makeURL(geneName, "XML"));
				Document doc = docBuilder.parse(url.openStream());
				doc.getDocumentElement().normalize();
				
				NodeList searchList = doc.getElementsByTagName("searchResponse");

				for (int temp = 0; temp < searchList.getLength(); temp++) {
					Node searchNode = searchList.item(temp);

					if (searchNode.getNodeType() == Node.ELEMENT_NODE) {
						Element searchElement = (Element) searchNode;

						try{
							omimobject.setGenesymbol(searchElement.getElementsByTagName("geneSymbols").item(0).getTextContent());
						} catch(Exception e){ omimobject.setGenesymbol("null"); }
						
						try{
							omimobject.setCytoloc(searchElement.getElementsByTagName("cytoLocation").item(0).getTextContent());
						} catch(Exception e){ omimobject.setCytoloc("null"); }
		
						try{
							omimobject.setGenestatus(searchElement.getElementsByTagName("confidence").item(0).getTextContent());
						} catch(Exception e){ omimobject.setGenestatus("null"); }
						
						try{
							omimobject.setGenemapmethods(searchElement.getElementsByTagName("mappingMethods").item(0).getTextContent());
						} catch (Exception e) { omimobject.setGenemapmethods("null"); }
		
						try{
							omimobject.setDisorderItem(searchElement.getElementsByTagName("phenotype").item(0).getTextContent());
						} catch (Exception e) { omimobject.setDisorderItem("null"); }
		
						try{
							omimobject.setMimIDItem(searchElement.getElementsByTagName("mimNumber").item(0).getTextContent());
						} catch (Exception e) { omimobject.setMimIDItem("null"); }
		
						try{
							omimobject.setCommentsItem(searchElement.getElementsByTagName("comments").item(0).getTextContent());
						} catch (Exception e){ omimobject.setCommentsItem("null"); }
		
						try{
							omimobject.setInheritanceItem(searchElement.getElementsByTagName("geneInheritance").item(0).getTextContent());
						} catch (Exception e) { omimobject.setInheritanceItem("null"); }
						
						try{
							omimobject.setPhenoMapMethodsItem(searchElement.getElementsByTagName("phenotypeMappingKey").item(0).getTextContent());
						} catch (Exception e) { omimobject.setPhenoMapMethodsItem("null"); }
		
						try{
							omimobject.setReferencesItem(searchElement.getElementsByTagName("references").item(0).getTextContent());
						} catch (Exception e) { omimobject.setReferencesItem("null"); }
					}
				}
			} catch (ParserConfigurationException e) {
				System.err.println("ERROR 3: ParserConfigurationException - readXML()");
				e.printStackTrace();
			} catch (MalformedURLException e) {
				System.err.println("ERROR 4: ERROR READING XML URL: MalformedURLException - readXML()");
				e.printStackTrace();
			} catch (SAXException e) {
				System.err.println("ERROR 5: SAXException - readXML()");
				System.err.println("polTable[4] setting to <ERROR> ...");
				omimobject.setGenemapmethods("<ERROR>"); 
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("ERROR 6: IOException - readXML()");
				e.printStackTrace();
			}
			
			return omimobject;
		}
		
		public String makeURL(String geneName, String report) {
			return 
			"http://api.omim.org/api/geneMap/search?search=" + geneName + "&filter=&fields=&retrieve=&start=0&limit=10&sort=&operator=&include=all&format=xml&apiKey=0807D10B6344A56E6C07AC19A6AF5A1957C6B92C";
		}
	}
}