package DiverData_Models;

public class Person implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String personID;
	private String personCode;
	private String familyID;
	private String fatherID;
	private String motherID;
	private boolean sex;
	private boolean liveSatuts;
		
	public String getPersonID() {
		return personID;
	}

	public void setPersonID(String personID) {
		this.personID = personID;
	}

	public String getPersonCode() {
		return personCode;
	}

	public void setPersonCode(String personCode) {
		this.personCode = personCode;
	}

	public String getFamilyID() {
		return familyID;
	}

	public void setFamilyID(String familyID) {
		this.familyID = familyID;
	}

	public String getFatherID() {
		return fatherID;
	}

	public void setFatherID(String fatherID) {
		this.fatherID = fatherID;
	}

	public String getMotherID() {
		return motherID;
	}

	public void setMotherID(String motherID) {
		this.motherID = motherID;
	}

	public boolean isSex() {
		return sex;
	}

	public void setSex(boolean sex) {
		this.sex = sex;
	}

	public boolean isLiveSatuts() {
		return liveSatuts;
	}

	public void setLiveSatuts(boolean liveSatuts) {
		this.liveSatuts = liveSatuts;
	}
}