package DiverData_Models;

public class Haplotype implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String haplotypeID;
	private String haplotypeAlleleNum;
	private String haplotypeValue;
	private String softwareMethod;
			
	public String getHaplotypeID() {
		return haplotypeID;
	}

	public void setHaplotypeID(String haplotypeID) {
		this.haplotypeID = haplotypeID;
	}

	public String getHaplotypeAlleleNum() {
		return haplotypeAlleleNum;
	}

	public void setHaplotypeAlleleNum(String haplotypeAlleleNum) {
		this.haplotypeAlleleNum = haplotypeAlleleNum;
	}

	public String getHaplotypeValue() {
		return haplotypeValue;
	}

	public void setHaplotypeValue(String haplotypeValue) {
		this.haplotypeValue = haplotypeValue;
	}

	public String getSoftwareMethod() {
		return softwareMethod;
	}

	public void setSoftwareMethod(String softwareMethod) {
		this.softwareMethod = softwareMethod;
	}
}