package BioData_Models;

import java.util.ArrayList;
import java.util.List;

public class OMIM implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String polymorphismCode;
	private String genesymbol;
	private String cytoloc;
	private String genestatus;
	private String genemapmethods;
	private List<String> disorder;
	private List<String> mimid;
	private List<String> inheritance;
	private List<String> phenomapmethods;
	private List<String> comments;
	private List<String> references;
	
	public OMIM(){
		disorder        = new ArrayList<String>();
		mimid           = new ArrayList<String>();
		inheritance     = new ArrayList<String>();
		phenomapmethods = new ArrayList<String>();
		comments        = new ArrayList<String>();
		references      = new ArrayList<String>();
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

	public List<String> getDisorder() {
		return disorder;
	}

	public void setDisorderItem(String i) {
		this.disorder.add(i);
	}

	public List<String> getComments() {
		return comments;
	}

	public void setCommentsItem(String i) {
		this.comments.add(i);
	}

	public List<String> getReferences() {
		return references;
	}

	public void setReferencesItem(String i) {
		this.references.add(i);
	}

	public List<String> getMimID() {
		return mimid;
	}

	public void setMimIDItem(String i) {
		this.mimid.add(i);
	}

	public List<String> getInheritance() {
		return inheritance;
	}

	public void setInheritanceItem(String i) {
		this.inheritance.add(i);
	}

	public List<String> getPhenoMapMethods() {
		return phenomapmethods;
	}

	public void setPhenoMapMethodsItem(String i) {
		this.phenomapmethods.add(i);
	}
	
	public String getCytoloc() {
		return cytoloc;
	}

	public void setCytoloc(String cytoloc) {
		this.cytoloc = cytoloc;
	}

	public String getGenestatus() {
		return genestatus;
	}

	public void setGenestatus(String genestatus) {
		this.genestatus = genestatus;
	}

	public String getGenemapmethods() {
		return genemapmethods;
	}

	public void setGenemapmethods(String genemapmethods) {
		this.genemapmethods = genemapmethods;
	}
}