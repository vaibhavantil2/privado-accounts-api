package ai.privado.demo.accounts.service.entity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "USERS")
public class UserE extends BaseE {

	private static final long serialVersionUID = 1L;

	@Column(name = "FIRST_NAME", length = 200)
	private String firstName;

	@Column(name = "LAST_NAME", length = 200)
	private String lastName;

	@Column(name = "EMAIL", length = 200)
	private String email;

	@Column(name = "PHONE", length = 200)
	private String phone;

	@Column(name = "PASSWORD", length = 200)
	private String password;

	@Column(name = "DOB", length = 200)
	private String dob;

	@Column(name = "Device_ID", length = 200)
	private String deviceid;

	@Column(name = "Device_Info", length = 200)
	private String deviceinfo;
	
	@Column(name = "Advertising_ID", length = 200)
	private String advertisingid;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}
	
	public String getDeviceID() {
		return deviceid;
	}

	public void setDeviceID(String deviceid) {
		this.deviceid = deviceid;
	}	

	public String getDeviceInfo() {
		return deviceinfo;
	}

	public void setDeviceInfo(String deviceinfo) {
		this.deviceinfo = deviceinfo;
	}	

	public String getAdvertisingID() {
		return advertisingid;
	}

	public void setAdvertisingID(String Advertisingid) {
		this.advertisingid = advertisingid;
	}

}
