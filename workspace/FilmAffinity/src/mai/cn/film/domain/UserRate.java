package mai.cn.film.domain;

public class UserRate extends Rate implements GraphNode {

	int iduser;
	int rate;
	
	public UserRate(int iduser, int rate){
		this.iduser=iduser;
		this.rate=rate;
	}

	public int getIduser() {
		return iduser;
	}

	public void setIduser(int iduser) {
		this.iduser = iduser;
	}

	public void setUserRate(int rate) {
		this.rate = rate;
	}

	@Override
	public int getId() {		
		return getIduser();
	}

	@Override
	public int getRate() {		
		return rate;
	}

	@Override
	public Type getType() {		
		return Type.USER;
	}

	@Override
	public String getLabel() {
		return "u"+getId();
	}

}
