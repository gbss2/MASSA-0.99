package DiverData_Models;

public class Phenotype implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String phenotype;
	private boolean affStatus;
	private float quantVariable;
	private String qualVariable;
	
	public String getPhenotype() {
		return phenotype;
	}
	
	public void setPhenotype(String phenotype) {
		this.phenotype = phenotype;
	}

	public boolean isAffStatus() {
		return affStatus;
	}

	public void setAffStatus(boolean affStatus) {
		this.affStatus = affStatus;
	}

	public float getQuantVariable() {
		return quantVariable;
	}

	public void setQuantVariable(float quantVariable) {
		this.quantVariable = quantVariable;
	}

	public String getQualVariable() {
		return qualVariable;
	}

	public void setQualVariable(String qualVariable) {
		this.qualVariable = qualVariable;
	}
}