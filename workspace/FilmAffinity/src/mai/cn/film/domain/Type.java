package mai.cn.film.domain;

public enum Type {
	
	MOVIE,USER, ME;
	
	public String toString(){
		switch(this){
		case MOVIE: 
			return "MOVIE";
		case USER: 
			return "USER";
		case ME: 
			return "ME";
		default: 
			return "";
		}
	}
	
}
