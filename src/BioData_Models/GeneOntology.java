package BioData_Models;

import java.util.ArrayList;
import java.util.List;

public class GeneOntology implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String polymorphismCode;
	private String genesymbol;
 	private List<String> molFunction;
	private List<String> celComponent;
	private List<String> bioProcess;
	
	public GeneOntology(){
		molFunction = new ArrayList<String>();
		celComponent= new ArrayList<String>();
		bioProcess  = new ArrayList<String>();
	}

	public String getPolymorphismCode() {
		return polymorphismCode;
	}

	public void setPolymorphismCode(String polymorphismCode) {
		this.polymorphismCode = polymorphismCode;
	}

	public String getGenesymbol() {
		return genesymbol;
	}

	public void setGenesymbol(String genesymbol) {
		this.genesymbol = genesymbol;
	}

	public List<String> getMolFunction() {
		return molFunction;
	}

	public void setMolFunctionItem(String i) {
		this.molFunction.add(i);
	}

	public List<String> getCelComponent() {
		return celComponent;
	}

	public void setCelComponentItem(String i) {
		this.celComponent.add(i);
	}

	public List<String> getBioProcess() {
		return bioProcess;
	}

	public void setBioProcessItem(String i) {
		this.bioProcess.add(i);
	}
}