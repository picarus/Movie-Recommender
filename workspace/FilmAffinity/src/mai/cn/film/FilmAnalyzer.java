package mai.cn.film;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Vector;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import mai.cn.film.db.FilmDB;

import mai.cn.film.domain.Affinity;
import mai.cn.film.domain.Film;
import mai.cn.film.domain.Graph;
import mai.cn.film.domain.MeMovieRate;
import mai.cn.film.domain.MovieRate;
import mai.cn.film.domain.MovieUsers;
import mai.cn.film.domain.UserMovies;
import mai.cn.film.domain.UserRate;

import mai.cn.utils.Utils;

public class FilmAnalyzer {
	
	SortedMap<Integer, MovieUsers> movieUsers, recMovieUsers;
	SortedMap<Integer, UserMovies> userMovies;
	UserMovies myRatedFilms;
	Graph graphUsers;
	Graph graphMovies;
	Graph graph;
	SortedMap<Integer, Film> films;
	int mode;
	boolean withMe;
	
	protected float rThr[]={0.8f,0.6f,0.4f,0.2f,0.0f};
	protected float rThrS[]={0.4f};
	
	// TODO
	// added nodes --> duplicated on console traces 3 times
	
	public FilmAnalyzer(int mode, boolean withMe){
		this.mode=mode;
		this.withMe=withMe;
	}
	
	public static void main(String[] args) {
			
		FilmAnalyzer faT,faF;				
		int mode =0;
		boolean withMe;
		
		FilmDB filmDB=FilmDB.getInstance();
		
		/////////////////////////////////////
		// Generate histogram for network
		////////////////////////////////////
		//int granularity=100;
		//ArrayList<Integer> histo=filmDB.selectVotedFilms(granularity);
		//Utils.print("histo"+granularity+".csv",histo);
		
		withMe=true;
		faT = new FilmAnalyzer(mode, withMe);
		faT.buildGraphs();
		
//		withMe=false;
//		faF = new FilmAnalyzer(mode, withMe);
//		faF.buildGraphs();
		
		// compare myrates vs average
//		faT.predictVSaverageMovieRate(); // both withMe: T/F are equal
//		faT.predictuserVSaverageMovieRate();
//		System.out.println("***********INCLUSIVE***********************");
//		// reconstruct my average vs user correlation
//		System.out.println("***********USERS***************************");
//		faT.predictVSusers();
//		System.out.println("***********MOVIES**************************");
//		faT.predictVSmovies();
//		faT.predictuserVSmovies();
//		System.out.println("***********REC MOVIES**********************");
//		faT.predictVSrecmovies();
//		
//		System.out.println("***********NON INCLUSIVE*******************");
//		// predict my average vs user correlation
//		System.out.println("***********MOVIES**************************");
//		faF.predictVSmovies();
		//faF.predictVSusers(); does not make sense, there are no affinities with me
		// predict my average vs user correlation partially generated	
		filmDB.close();
	}

	private int FACTOR_LENGTH_TESTS=4;
	
	public void predictVSusers( ){

		SortedMap<Integer,MovieRate> mrs=myRatedFilms.getMovieRates();
		int idx=2;
		int idmovie;
		int myrate;
		double diffS;
		MeMovieRate mmr;
		int size = rThr.length * FACTOR_LENGTH_TESTS;
		Vector<Integer> nMatch = initVectorZeroI(size);
		Vector<Integer> nEl = initVectorZeroI(size);
		Vector<Double> mse = initVectorZeroD(size);
		Vector<Double> mae = initVectorZeroD(size);
		SimpleRegression srs= new SimpleRegression();
		Vector<SimpleRegression> sr = initVectorSR(size);
		Vector<Double> rateLocal;
		Vector<Integer> nMatchLocal;
		int n=0;
		for (MovieRate mr:mrs.values()){ // for each movie in my list
			rateLocal = initVectorZeroD(size);
			nMatchLocal=initVectorZeroI(size);
			mmr =(MeMovieRate) mr;
			idmovie = mmr.getIdmovie();
			myrate = mmr.getRate(); // target
			socialUserRate( idmovie, size, rateLocal, nMatchLocal ); 
			updateVectors( mse, mae, sr, rateLocal, nMatch, nMatchLocal, nEl, myrate);
			
			if (!rateLocal.get(idx).isNaN()){
				diffS = myrate-rateLocal.get(idx);
				float rate=getAvgRate(idmovie);
				float diffA=myrate-rate;
				srs.addData(diffS,diffA);
				if (diffS*diffA<0.0){
					n++;
				}
			}
		}
		System.out.println("R:"+srs.getR()+" N Off:"+n);
		sizeVectors( mse, mae, nEl);
		printVector( mse, mae, sr, nEl, nMatch);	
	}	
	
	public void predictuserVSmovies(){
		int idmovie;
		int myrate;
		double diffS;
		int idx=0;
		int size=rThrS.length;
		
		printVectorHeaderWUser();
		
		for (UserMovies um:userMovies.values()){
		
			SortedMap<Integer,MovieRate> mrs=um.getMovieRates();
		
			Vector<Integer> nMatch = initVectorZeroI(size);
			Vector<Integer> nEl = initVectorZeroI(size);
			Vector<Double> mse = initVectorZeroD(size);
			Vector<Double> mae = initVectorZeroD(size);
			SimpleRegression srs = new SimpleRegression();
			Vector<SimpleRegression> sr = initVectorSR(size);
			Vector<Double> rateLocal;
			Vector<Integer> nMatchLocal;
			
			for (MovieRate mr:mrs.values()){ // for each movie in the user's list
				rateLocal = initVectorZeroD(size);
				nMatchLocal=initVectorZeroI(size);
				
				idmovie = mr.getIdmovie();
				myrate = mr.getRate(); // target
				socialMovieRateS( idmovie, size, rateLocal, nMatchLocal); 
				updateVectors( mse, mae, sr, rateLocal, nMatch, nMatchLocal, nEl, myrate);
				if ( !rateLocal.get(idx).isNaN() && nMatchLocal.get(idx)>0 ){
					diffS = myrate-rateLocal.get(idx);
					float rate = getAvgRate(idmovie);
					float diffA = myrate-rate;
					srs.addData(diffS,diffA);
				}
			}
			
			sizeVectors(mse,mae,nEl);
			printVectorDataWUser(um.getId(),mse,mae,sr, nEl, nMatch);
		}
	}
	
	
	public void predictuserVSaverageMovieRate() {
		
		int idmovie;
		Film film;
		float rate, diff;
		int userRate;
		double rmse;
		
		System.out.println("\t USER \t MSE \t\t\t MAE \t\t\t RMSE \t\t\t SR \t\t\t N");
		
		for (UserMovies um:userMovies.values()){
			SortedMap<Integer,MovieRate> mrs=um.getMovieRates();
			double mse=0.0d;
			double mae=0.0d;
			int total=0;
			SimpleRegression sr=new SimpleRegression();
			for (MovieRate mr:mrs.values()){
				idmovie=mr.getIdmovie();
				film=films.get(idmovie);
				if (film!=null){
					total++;
					rate=film.getAvgRate();
					userRate=mr.getRate();
					diff=rate-userRate;
					mse+=Math.pow(diff,2);
					mae+=Math.abs(diff);
					sr.addData(diff, userRate);
				}
			}
			mse/=total;
			mae/=total;
			rmse=Math.sqrt(mse);
			System.out.println("\t"+um.getId()+"\t"+mse+"\t"+mae+"\t"+rmse+"\t"+sr.getR()+"\t"+sr.getN());
		}
	}
	
	public void predictVSrecmovies(){

		FilmDB filmDB=FilmDB.getInstance();
			
		int idmovie;
		int idx=2;
			
		int size=rThr.length*FACTOR_LENGTH_TESTS;
		Vector<Double> rateLocal;
		Vector<Integer> nMatchLocal;
		float rate=0.0f;

		System.out.println("Id\tTitle\tSocial Rate\tAvg.Rate\tN Match");
		for (MovieUsers mu:recMovieUsers.values()){ // for each movie in the list of recommended movies
			rateLocal = initVectorZeroD(size);
			nMatchLocal=initVectorZeroI(size);
			
			idmovie = mu.getId();
			
			socialMovieRate( idmovie, size, rateLocal, nMatchLocal ); 

			if (!rateLocal.get(idx).isNaN()){				
				rate=getAvgRate(idmovie);				
			}
			if (!rateLocal.get(idx).isNaN())
				System.out.println(idmovie+"\t"+filmDB.getMovieTitle(idmovie)+"\t"+rateLocal.get(idx)+"\t"+rate+"\t"+nMatchLocal.get(idx));
		}
	}
			
	public void predictVSmovies(){
		FilmDB filmDB=FilmDB.getInstance();
		SortedMap<Integer,MovieRate> mrs=myRatedFilms.getMovieRates();
	
		int idmovie;
		int idx=2;
		int myrate;
		double diffS=0.0;
		MeMovieRate mmr;
		int size=rThr.length*FACTOR_LENGTH_TESTS;
		Vector<Integer> nMatch = initVectorZeroI(size);
		Vector<Integer> nEl = initVectorZeroI(size);
		Vector<Double> mse = initVectorZeroD(size);
		Vector<Double> mae = initVectorZeroD(size);
		SimpleRegression srs = new SimpleRegression();
		Vector<SimpleRegression> sr = initVectorSR(size);
		Vector<Double> rateLocal;
		Vector<Integer> nMatchLocal;
		int n=0;
		float rate=0.0f;
		float diffA=0.0f;
		System.out.println("Id\tTitle\tDiffS\tDiffA\tMy Rate\tSocial Rate\tAvg.Rate\tN Match");
		for (MovieRate mr:mrs.values()){ // for each movie in my list
			rateLocal = initVectorZeroD(size);
			nMatchLocal=initVectorZeroI(size);
			mmr =(MeMovieRate) mr;
			idmovie = mmr.getIdmovie();
			myrate = mmr.getRate(); // target
			socialMovieRate( idmovie, size, rateLocal, nMatchLocal ); 
			updateVectors( mse, mae, sr, rateLocal, nMatch, nMatchLocal, nEl, myrate);
			if (!rateLocal.get(idx).isNaN()){
				diffS = myrate-rateLocal.get(idx);
				rate=getAvgRate(idmovie);
				diffA=myrate-rate;
				srs.addData(diffS,diffA);
				if (diffS*diffA<0.0){
					n++;					
				}
			}
			if (!rateLocal.get(idx).isNaN())
				System.out.println(idmovie+"\t"+filmDB.getMovieTitle(idmovie)+"\t"+diffS+"\t"+diffA+"\t"+myrate+"\t"+rateLocal.get(idx)+"\t"+rate+"\t"+nMatchLocal.get(idx));
		}
		System.out.println("R:"+srs.getR()+" N Off:"+n);
		sizeVectors(mse,mae,nEl);
		printVector(mse,mae,sr, nEl, nMatch);
	}
	
	public float getAvgRate(int idmovie){
		float rate;
		Film film=films.get(idmovie);
		rate=film.getAvgRate();
		return rate;
	}
	
	private void updateVectors( Vector<Double> mse, 		 Vector<Double> mae, 
								Vector<SimpleRegression> sr, Vector<Double> rate, 
								Vector<Integer> nMatch, 	 Vector<Integer> nMatchLocal,
								Vector<Integer> nEl,		 int myrate ){
		double diff;
		Double rateValue;
		double maeValue, mseValue;
		SimpleRegression srElem;
		int s=mse.size();
		
		for (int i=0; i<s; i++){
			rateValue = rate.get(i);
			if ( nMatchLocal.get(i) > 0 ){
				diff = myrate-rateValue;
				mseValue = mse.get(i)+Math.pow(diff,2);
				mse.set( i, mseValue);
				maeValue = mae.get(i)+Math.abs(diff);
				mae.set( i, maeValue);
				srElem = sr.get(i);
				srElem.addData( myrate, diff );
				nEl.set( i, nEl.get(i)+1 );
				nMatch.set( i, nMatch.get(i) + nMatchLocal.get(i) );
			}
		}
	}
	
	private void sizeVectors(Vector<Double> mse, Vector<Double> mae, Vector<Integer> nEl){
		int size=mse.size();
		int n;
		for (int i=0;i<size;i++){
			n=nEl.get(i);
			if (n>0) {
				mae.set(i,mae.get(i)/n);
				mse.set(i,mse.get(i)/n);
			}
		}
	}
	
	private Vector<Integer> initVectorZeroI(int size){
		Vector<Integer> zero=new Vector<Integer>(size);
		for (int i=0;i<size;i++) zero.add(0);
		return zero;
	}
	
	private Vector<Double> initVectorZeroD(int size){
		Vector<Double> zero=new Vector<Double>(size);
		for (int i=0;i<size;i++) zero.add(0.0d);
		return zero;
	}
	
	private Vector<SimpleRegression> initVectorSR(int size){
		Vector<SimpleRegression> sr=new Vector<SimpleRegression>(size);
		for (int i=0;i<size;i++) sr.add(new SimpleRegression());
		return sr;
	}
	
	private void printVector(Vector<Double> mse, Vector<Double> mae, Vector<SimpleRegression> sr, Vector<Integer> nEl, Vector<Integer> nMatch){
		printVectorHeader();
		printVectorData(mse, mae, sr, nEl, nMatch);
	}

	
	protected void printVectorDataWUser(int idUser, Vector<Double> mse, Vector<Double> mae,
			Vector<SimpleRegression> sr, Vector<Integer> nEl, Vector<Integer> nMatch) {
		int size=mse.size();
		for (int i=0;i<size;i++){ 
			System.out.println("\t"+idUser+"\t"+rThrS[i % rThrS.length]+"\t"+mse.get(i)+"\t"+mae.get(i)+"\t"+Math.sqrt(mse.get(i))+"\t"+sr.get(i).getR()+"\t"+nEl.get(i)+"\t"+nMatch.get(i));
		}
	}
	
	protected void printVectorData(Vector<Double> mse, Vector<Double> mae,
			Vector<SimpleRegression> sr, Vector<Integer> nEl,
			Vector<Integer> nMatch) {
		int size=mse.size();
		for (int i=0;i<size;i++){ 
			System.out.println(rThr[i % rThr.length]+"\t"+mse.get(i)+"\t"+mae.get(i)+"\t"+Math.sqrt(mse.get(i))+"\t"+sr.get(i).getR()+"\t"+nEl.get(i)+"\t"+nMatch.get(i));
		}
	}

	protected void printVectorHeaderWUser() {
		System.out.println("USER \tTHR \tMSE \t\t\tMAE \t\t\tRMSE \t\t\tSR \t\t\tN \tNMATCH");
	}
	
	
	protected void printVectorHeader() {
		System.out.println("THR \tMSE \t\t\tMAE \t\t\tRMSE \t\t\tSR \t\t\tN \tNMATCH");
	}

	protected void socialMovieRateS(int idmovie, int size, Vector<Double> rate, Vector<Integer> nMatch ){
		
		Iterator<Affinity> itAff;
		Vector<Double> denom = initVectorZeroD(size);
		Affinity aff;
		double metric;
		int id2movie;
		float rL;
		Film film = films.get(idmovie); // the film to calculate the rate
		if (film!=null){
			float deltaRate2;
			itAff = film.getAffinities(); // list of MOVIE affinities
			int segSize = rThrS.length;
			MovieUsers movieUsers2, movieUsers1=movieUsers.get(idmovie);
			float myRate2, avgRate2, avgRate = movieUsers1.getAvgRate();
			
			while (itAff.hasNext()){
				
				aff=itAff.next();
				id2movie=aff.getOtherLabel(idmovie);
				
				myRate2 = myRatedFilms.getRateForMovie(id2movie); // my rate for the other movie
				
				if (myRate2>=0){
					
					movieUsers2 = movieUsers.get(id2movie);
					avgRate2 = movieUsers2.getAvgRate(); // avg rate from similar users.
					deltaRate2 = avgRate2 - myRate2;
					metric = aff.getMetric(); // correlation among movies
					
					for (int i=0; i < segSize; i++){
						rL = rThrS[i];
						if (metric > rL){
							updateRate( rate, denom, i, metric, deltaRate2, nMatch );	
						}
					}
				}	
			}
			calculateRate(rate, denom, avgRate);
		}
	}
	
	protected void socialMovieRate(int idmovie, int size, Vector<Double> rate, Vector<Integer> nMatch ){
		
		Iterator<Affinity> itAff;
		Vector<Double> denom = initVectorZeroD(size);
		Affinity aff;
		double metric, absMetric;
		int id2movie;
		float rL, rU;
		Film film = films.get(idmovie); // the film to calculate the rate
		float deltaRate2;
		itAff = film.getAffinities(); // list of MOVIE affinities
		int segSize = rThr.length;
		MovieUsers movieUsers2, movieUsers1=movieUsers.get(idmovie);
		float myRate2, avgRate2, avgRate = movieUsers1.getAvgRate();
		
		while (itAff.hasNext()){
			
			aff=itAff.next();
			id2movie=aff.getOtherLabel(idmovie);
			
			myRate2 = myRatedFilms.getRateForMovie(id2movie); // my rate for the other movie
			
			if (myRate2>=0){
				
				movieUsers2 = movieUsers.get(id2movie);
				avgRate2 = movieUsers2.getAvgRate(); // avg rate from similar users.
				deltaRate2 = avgRate2 - myRate2;
				metric = aff.getMetric(); // correlation among movies
				absMetric = Math.abs(metric);
				rU = 1.0f;
				for (int i=0; i < segSize; i++){
					rL = rThr[i];
					
					if (metric > rL){
						updateRate( rate, denom, i, metric, deltaRate2, nMatch );	
						//System.out.println("avgRate:"+avgRate+" avgRate2:"+avgRate2+" myRate:"+myRate2+" deltaRate:"+deltaRate2+" rate:"+rate+" metric:"+metric);
						if (metric < rU){
							updateRate( rate, denom, i+segSize*2, metric, deltaRate2, nMatch );
						} 
					}
					
					if (absMetric > rL){						
						updateRate( rate, denom, segSize+i, metric, deltaRate2, nMatch );						
						if (absMetric < rU){
							updateRate( rate, denom, i+segSize*3, metric, deltaRate2, nMatch );
						}						
					}
					
					rU = rL; // they are in descending order
				}
			}	
		}
		
		calculateRate(rate, denom, avgRate);
		
	}

	protected void socialMovieRateOld(int idmovie, int size, Vector<Double> rate, Vector<Integer> nMatch ){
		
		Iterator<Affinity> itAff;
		Vector<Double> denom = initVectorZeroD(size);
		Affinity aff;
		double metric, absMetric;
		int id2movie;
		float rL, rU;
		Film film = films.get(idmovie); // the film to calculate the rate
		float myRate2, avgRate2, avgRate = film.getAvgRate();
		float deltaRate2;
		itAff = film.getAffinities(); // list of MOVIE affinities
		int segSize = rThr.length;
		MovieUsers movieUsers2;
		
		while (itAff.hasNext()){
			
			aff=itAff.next();
			id2movie=aff.getOtherLabel(idmovie);
			
			myRate2 = myRatedFilms.getRateForMovie(id2movie); // my rate for the other movie
			
			if (myRate2>=0){
				
				movieUsers2 = movieUsers.get(id2movie);
				avgRate2 = movieUsers2.getAvgRate(); // avg rate from similar users.
				deltaRate2 = avgRate2 - myRate2;
				metric = aff.getMetric(); // correlation among movies
				absMetric = Math.abs(metric);
				rU = 1.0f;
				for (int i=0; i < segSize; i++){
					rL = rThr[i];
					
					if (metric > rL){
						updateRate( rate, denom, i, metric, deltaRate2, nMatch );	
						System.out.println("avgRate:"+avgRate+" avgRate2:"+avgRate2+" myRate:"+myRate2+" deltaRate:"+deltaRate2+" rate:"+rate+" metric:"+metric);
						if (metric < rU){
							updateRate( rate, denom, i+segSize*2, metric, deltaRate2, nMatch );
						} 
					}
					
					if (absMetric > rL){						
						updateRate( rate, denom, segSize+i, metric, deltaRate2, nMatch );						
						if (absMetric < rU){
							updateRate( rate, denom, i+segSize*3, metric, deltaRate2, nMatch );
						}						
					}
					
					rU = rL; // they are in descending order
				}
			}
			
		}
		
		calculateRate(rate, denom, avgRate);
		
	}
	
	protected Vector<Double> socialUserRate(int iduser, int size, Vector<Double> rate, Vector<Integer> nMatch ){
		
		// for each movie
		// find the users with closer ratings to my ratings
		// if they have rated the movie
		// add their rate to the "average" multiplied by correlation factor
		
		Vector<Double> denom=initVectorZeroD(size);
		Affinity aff;
		double metric, absMetric;
		int id2user;
		float rL, rU;
		MovieUsers mu = movieUsers.get(iduser); 				// the film to calculate the rate
		float deltaRate2,avgRate = mu.getAvgRate();		   		// avg. movie rate (from users, soulmates)
		Iterator<Affinity> itAff = myRatedFilms.getAffinities();// these are the user affinities with me
		int segSize=rThr.length;
		UserMovies um;
		int userRate;
		
		while (itAff.hasNext()){
			
			aff=itAff.next();
			id2user=aff.getOtherLabel(0); // my id is 0, I want the other user id
			
			um=userMovies.get(id2user);
			
			if (um!=null){
				
				userRate=um.getRateForMovie(iduser);
				
				if (userRate>=0){
					metric = aff.getMetric(); // correlation among the other user and myself
					absMetric = Math.abs(metric);
					deltaRate2 = avgRate-userRate;
					
					rU=1.0f;
					for (int i=0; i < segSize; i++){
						rL = rThr[i];
						
						if (metric > rL){
							//System.out.println("iduser:"+iduser+" id2user:"+id2user+" deltaRate:"+deltaRate2);
							updateRate( rate, denom, i, metric, deltaRate2, nMatch );
							if (metric < rU){
								updateRate( rate, denom, i+segSize*2, metric, deltaRate2, nMatch );
							}
						}
						
						if (absMetric > rL){
							updateRate( rate, denom, segSize+i, metric, deltaRate2, nMatch );
							if (absMetric < rU){
								updateRate( rate, denom, i+segSize*3, metric, deltaRate2, nMatch );
							}
						}
						rU=rL;
					}
				}
			}
		}
		
		calculateRate(rate, denom, avgRate);
		
		return rate;
	}
	
	private void updateRate(Vector<Double> rate, Vector<Double> denom, int i, double metric, double delta, Vector<Integer> nMatch){
		rate.set( i, rate.get(i) + metric * delta);
		denom.set( i, denom.get(i) + Math.abs(metric));
		nMatch.set( i, nMatch.get(i)+1);
	}
	
	private void calculateRate(Vector<Double> rate, Vector<Double> denom, float avgRate){
		int size=rate.size();
		for (int i=0; i<size; i++){
			rate.set(i,avgRate-rate.get(i)/denom.get(i)); 
		}
	}
	
	public void predictVSaverageMovieRate() {
		SortedMap<Integer,MovieRate> mrs=myRatedFilms.getMovieRates();
		int idmovie;
		Film film;
		float rate;
		int myrate;
		MeMovieRate mmr;
		float diff;
		double mse=0.0d;
		double mae=0.0d;
		int total=0;
		SimpleRegression sr=new SimpleRegression();
		for (MovieRate mr:mrs.values()){
			mmr=(MeMovieRate) mr;
			idmovie=mr.getIdmovie();
			film=films.get(idmovie);
			if (film!=null){
				total++;
				rate=film.getAvgRate();
				mmr.setAvgRate(rate);
				myrate=mr.getRate();
				diff=rate-myrate;
				mse+=Math.pow(diff,2);
				mae+=Math.abs(diff);
				sr.addData(diff, myrate);
			}
		}
		mse/=total;
		mae/=total;
		double rmse=Math.sqrt(mse);
		
		System.out.println("\t MSE \t\t\t MAE \t\t\t RMSE \t\t\t SR \t\t\t N");
		System.out.println("\t"+mse+"\t"+mae+"\t"+rmse+"\t"+sr.getR()+"\t"+sr.getN());
	}
	
	public void buildGraphs() {
		FilmDB filmDB=FilmDB.getInstance();
		movieUsers=filmDB.selectMovieRates("matesmovies");
		userMovies=filmDB.selectUserRates("matesmovies");
		films=filmDB.selectRatedFilms(true);
		myRatedFilms=filmDB.selectMyRates(true);
		recMovieUsers=filmDB.selectMovieRates("matesrec");
		createGraphs();
	}

	protected void createGraphs() {
		
		switch(mode){	
			case 1:
				graphAll();
				break;
			case 2:
				graphUsers();
				break;
			case 3:
				graphMovies();
				break;
			default:
				graphAll();
				graphUsers();
				graphMovies();
		}
	}

	protected void graphMovies( ) {
		graphMovies=new Graph(withMe, films);
		graphMovies.addAllMovies(myRatedFilms, userMovies, movieUsers);
		graphMovies.generatePajekMovies("famovies"+(withMe?"T":"F")+".net");
	}
	
	protected void graphUsers( ) {
		graphUsers=new Graph(withMe, films);
		graphUsers.addAllUsers(myRatedFilms, userMovies, movieUsers);
		graphUsers.generatePajekUsers("fausers"+(withMe?"T":"F")+".net");
	}

	protected void graphAll() {
		graph=new Graph(withMe, films);
		graph.addAll(myRatedFilms, userMovies, movieUsers);		
		graph.generatePajek("fa"+(withMe?"T":"F")+".net");
	}
	
}
