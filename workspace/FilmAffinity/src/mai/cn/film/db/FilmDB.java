package mai.cn.film.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import mai.cn.film.domain.Film;
import mai.cn.film.domain.MeMovieRate;
import mai.cn.film.domain.UserRate;
import mai.cn.film.domain.MovieUsers;
import mai.cn.film.domain.MyRatedFilm;
import mai.cn.film.domain.UserMovies;
import mai.cn.film.domain.MovieRate;
import mai.cn.film.domain.UserRecommendedFilm;

public class FilmDB {
	
	Connection conn;
	Statement st;
	int nErrorsDB=0;
	
	static FilmDB filmDB=null;
	
	public static FilmDB getInstance(){
		if (filmDB==null)
			filmDB = new FilmDB();
		
		return filmDB;	
	}
	
	private FilmDB(){
		createDBConn();
	}
	

	void createDBConn(){
		conn = null;
		
		try {
		    conn = DriverManager.getConnection("jdbc:mysql://localhost/filmaffinity?" +
		                                   "user=root&password=root");			  
		} catch (SQLException ex) {
		    // handle any errors
		    System.out.println("SQLException: " + ex.getMessage());
		    System.out.println("SQLState: " + ex.getSQLState());
		    System.out.println("VendorError: " + ex.getErrorCode());
		    conn=null;
		}
		
	}
	
	public void close(){
		try {
			conn.close();
		} catch (SQLException e) {			
			e.printStackTrace();
		}
	}
	
	public void commitTrans(){
			
		try {			
			conn.commit();			
		} catch (SQLException e) {			
			e.printStackTrace();
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			} 
		}				
	}
	
	public void closeConn(){
		try {
			conn.close();
		} catch (SQLException e2) {				
			e2.printStackTrace();
		}			
	}

	Statement openTrans(){
		
		try {
			conn.setAutoCommit(false);
			st=conn.createStatement();
			
		} catch (SQLException e) {			
			e.printStackTrace();
			st=null;
		}		
		return st;
	}
	
	public UserMovies selectMyRates(boolean rated){
		
		Statement stmt = null;
		ResultSet rs = null;
		
		UserMovies myfilms=new UserMovies(0); // my id is 0 (to be the first on the list)
		MovieRate movieRate;
		Integer idmovie;
		int myrate;		
		try {
		    stmt = conn.createStatement();
		    
		    rs = stmt.executeQuery("SELECT * FROM mymovies WHERE "+(rated?"myrate>=0":"myrate<0")+" ORDER BY idmovie");
		    
		    while (rs.next()) {
		    	idmovie=rs.getInt(1);
		    	myrate=rs.getInt(2);		    	
		    	movieRate=new MeMovieRate(idmovie,myrate);		    	
		    	myfilms.addMovieRate(movieRate);
		    }
		    
		    System.out.println("I have "+(rated?"":"NOT ")+"rated "+myfilms.numberOfRates()+" movies.");
		    
		}
		catch (SQLException ex){
		    System.err.println("SQLException: " + ex.getMessage());
		    System.err.println("SQLState: " + ex.getSQLState());
		    System.err.println("VendorError: " + ex.getErrorCode());
		}
		finally {
		   
		    if (rs != null) {
		        try {
		            rs.close();
		        } catch (SQLException sqlEx) { } // ignore

		        rs = null;
		    }

		    if (stmt != null) {
		        try {
		            stmt.close();
		        } catch (SQLException sqlEx) { } // ignore

		        stmt = null;
		    }
		}
		return myfilms;
	}
	
	public SortedMap<Integer,UserMovies> selectUserRates(String table){
		
		Statement stmt = null;
		ResultSet rs = null;		
		SortedMap<Integer,UserMovies> ums=new TreeMap<Integer,UserMovies>();
		Integer iduser;
		Vector<Integer> users=new Vector<Integer>();
		int idmovie;
	    int rate;
	    UserMovies userMovies;
	    MovieRate movieRate;
		int nor,totalnor;
		
		try {
		    stmt = conn.createStatement();
		    
		    rs = stmt.executeQuery("SELECT DISTINCT iduser FROM "+table+" ORDER BY iduser");
		    		    
		    while (rs.next()) {
		    	iduser=rs.getInt(1);		    	   
		    	users.add(iduser);  	
		    }
		    
		    rs.close();
		    
		    totalnor=0;
		    for (Integer user: users) {		    			    
			    rs = stmt.executeQuery("SELECT * FROM "+table+" WHERE iduser="+user+" ORDER BY idmovie");
			    userMovies=new UserMovies(user);
			    while (rs.next()) {
			    	idmovie=rs.getInt(1);
			    	iduser=rs.getInt(2);
			    	rate=rs.getInt(3); 
			    	movieRate=new MovieRate(idmovie,rate);
			    	userMovies.addMovieRate(movieRate);
			    }
			    nor=userMovies.numberOfRates();
			    ums.put(userMovies.getId(),userMovies);
			    System.out.println("User:"+user+" has "+nor+" movies.");
			    totalnor+=nor;
			    rs.close();
			    rs=null;		    
		    }
		        
		    System.out.println("In total "+users.size()+" users have rated "+totalnor+" movies");
		}
		catch (SQLException ex){		   
		    System.err.println("SQLException: " + ex.getMessage());
		    System.err.println("SQLState: " + ex.getSQLState());
		    System.err.println("VendorError: " + ex.getErrorCode());
		}
		finally {
		   
		    if (rs != null) {
		        try {
		            rs.close();
		        } catch (SQLException sqlEx) { } // ignore

		        rs = null;
		    }

		    if (stmt != null) {
		        try {
		            stmt.close();
		        } catch (SQLException sqlEx) { } // ignore

		        stmt = null;
		    }
		}
		return ums;
	}
	
	public String getMovieTitle(int idmovie){
		Statement stmt = null;
		ResultSet rs = null;		
			
		String title=null;
		
		try {
		    stmt = conn.createStatement();
		    
		    rs = stmt.executeQuery("SELECT 	movietitle FROM movietitle WHERE idmovie="+idmovie);
		    		    
		    while (rs.next()) {
		    	title=rs.getString(1);	  	
		    }
		    
		}
		catch (SQLException ex){
		    // handle any errors
		    System.err.println("SQLException: " + ex.getMessage());
		    System.err.println("SQLState: " + ex.getSQLState());
		    System.err.println("VendorError: " + ex.getErrorCode());
		}
		finally {
		    // it is a good idea to release
		    // resources in a finally{} block
		    // in reverse-order of their creation
		    // if they are no-longer needed

		    if (rs != null) {
		        try {
		            rs.close();
		        } catch (SQLException sqlEx) { } // ignore

		        rs = null;
		    }

		    if (stmt != null) {
		        try {
		            stmt.close();
		        } catch (SQLException sqlEx) { } // ignore

		        stmt = null;
		    }
		}
		return title;
	}
	
	
	public SortedMap<Integer,MovieUsers> selectMovieRates(String table){
		
		Statement stmt = null;
		ResultSet rs = null;		
		SortedMap<Integer,MovieUsers> mus=new TreeMap<Integer,MovieUsers>();		
		Vector<Integer> movies=new Vector<Integer>();
		int iduser;
		Integer idmovie;
	    int rate;
	    MovieUsers movieUsers;
	    UserRate movieRate;
		int nor,totalnor;
		
		try {
		    stmt = conn.createStatement();
		    
		    rs = stmt.executeQuery("SELECT DISTINCT idmovie FROM "+table+" ORDER BY idmovie");
		    		    
		    while (rs.next()) {
		    	idmovie=rs.getInt(1);		    	   
		    	movies.add(idmovie);  	
		    }
		    
		    rs.close();
		    
		    totalnor=0;
		    for (Integer movie: movies) {		    			    
			    rs = stmt.executeQuery("SELECT * FROM "+table+" WHERE idmovie="+movie+" ORDER BY iduser");
			    movieUsers=new MovieUsers(movie);
			    while (rs.next()) {
			    	idmovie=rs.getInt(1);
			    	iduser=rs.getInt(2);
			    	rate=rs.getInt(3); 
			    	movieRate=new UserRate(iduser,rate);
			    	movieUsers.addMovieRate(movieRate);
			    }
			    nor=movieUsers.numberOfRates();
			    mus.put(movieUsers.getId(),movieUsers);
			    System.out.println("Movie:"+movie+" has been rated by "+nor+" users.");
			    totalnor+=nor;
			    rs.close();
			    rs=null;		    
		    }
		        
		    System.out.println("In total "+movies.size()+" movies have been rated "+totalnor+" times");
		}
		catch (SQLException ex){
		    // handle any errors
		    System.err.println("SQLException: " + ex.getMessage());
		    System.err.println("SQLState: " + ex.getSQLState());
		    System.err.println("VendorError: " + ex.getErrorCode());
		}
		finally {
		    // it is a good idea to release
		    // resources in a finally{} block
		    // in reverse-order of their creation
		    // if they are no-longer needed

		    if (rs != null) {
		        try {
		            rs.close();
		        } catch (SQLException sqlEx) { } // ignore

		        rs = null;
		    }

		    if (stmt != null) {
		        try {
		            stmt.close();
		        } catch (SQLException sqlEx) { } // ignore

		        stmt = null;
		    }
		}
		return mus;
	}
	

	public ArrayList<Integer> selectVotedFilms(int granularity){
		Statement stmt = null;
		ResultSet rs = null;
		int votes;
		int upL,downL=0,max;
		ArrayList<Integer> histo=new ArrayList<Integer>();
		
		try {
		    stmt = conn.createStatement();
		    
		    rs = stmt.executeQuery("SELECT MAX(votes) FROM movie");
			
		    rs.first();
		    max=rs.getInt(1);
		    downL=-granularity+1;
		    do {
		    	upL=downL+granularity-1;
		    	rs.close();
		    	rs = stmt.executeQuery("SELECT COUNT(*) FROM movie WHERE votes>="+downL+" AND votes<="+upL);
			    rs.first();
			    votes=rs.getInt(1);
			    histo.add(votes);
			    System.out.println(downL+"-"+upL+":"+votes);
			    downL=upL+1;
		    }while (downL<max);
		       
		} catch (SQLException ex){
		    // handle any errors
		    System.err.println("SQLException: " + ex.getMessage());
		    System.err.println("SQLState: " + ex.getSQLState());
		    System.err.println("VendorError: " + ex.getErrorCode());
		} finally {
		    // it is a good idea to release
		    // resources in a finally{} block
		    // in reverse-order of their creation
		    // if they are no-longer needed
	
		    if (rs != null) {
		        try {
		            rs.close();
		        } catch (SQLException sqlEx) { } // ignore
	
		        rs = null;
		    }
	
		    if (stmt != null) {
		        try {
		            stmt.close();
		        } catch (SQLException sqlEx) { } // ignore
	
		        stmt = null;
		    }
		}
		return histo;
	}



	public SortedMap<Integer,Film> selectRatedFilms(boolean voted){
		Statement stmt = null;
		ResultSet rs = null;
		Film film;
		SortedMap<Integer,Film> hmfilm=new TreeMap<Integer,Film>();
		Integer idmovie;
		float avgrate;
		int votes;
		try {
		    stmt = conn.createStatement();
		    
		    rs = stmt.executeQuery("SELECT * FROM movie WHERE "+(voted?"votes>0":"votes=0")+" ORDER BY idmovie");

		    rs.first();
		    
		    while (rs.next()) {
		    	idmovie=rs.getInt(1);
		    	avgrate=rs.getFloat(2);
		    	votes=rs.getInt(3);
		    	film=new Film(idmovie,avgrate,votes);		    	
		    	hmfilm.put(idmovie,film);
		    }
		    
		    System.out.println("There are "+hmfilm.size()+" movies "+(voted?"":"NOT ")+"rated.");
		    
		    // or alternatively, if you don't know ahead of time that
		    // the query will be a SELECT...
		    // Now do something with the ResultSet ....
		}
		catch (SQLException ex){
		    // handle any errors
		    System.err.println("SQLException: " + ex.getMessage());
		    System.err.println("SQLState: " + ex.getSQLState());
		    System.err.println("VendorError: " + ex.getErrorCode());
		}
		finally {
		    // it is a good idea to release
		    // resources in a finally{} block
		    // in reverse-order of their creation
		    // if they are no-longer needed

		    if (rs != null) {
		        try {
		            rs.close();
		        } catch (SQLException sqlEx) { } // ignore

		        rs = null;
		    }

		    if (stmt != null) {
		        try {
		            stmt.close();
		        } catch (SQLException sqlEx) { } // ignore

		        stmt = null;
		    }
		}
		return hmfilm;
	}
	
	public void batchInsertURF(List<UserRecommendedFilm> lurf, String table){
		int i=0;
		Iterator<UserRecommendedFilm> it=lurf.iterator();
		UserRecommendedFilm film=null;
		PreparedStatement stmt;
		try {
			conn.setAutoCommit(false);
			stmt = conn.prepareStatement("INSERT INTO "+table+" VALUES (?, ?, ?)");
			while ( it.hasNext() ){
				film=it.next();						
				stmt.setInt(1, film.getFilmId());
				stmt.setInt(2, film.getUser());
				stmt.setInt(3, film.getUserRate());
				stmt.addBatch();		
				i++;
				if (i == 2000){
					// submit the batch for execution
					stmt.executeBatch();
					i=0;
				}				
			}			
			if (i>0){
				stmt.executeBatch();
			}
			conn.commit();			
		} catch(SQLException e){
			System.err.println("SQLException: " + e.getMessage());
			System.err.println("UserRecommendedFilm:"+film);
			nErrorsDB++;
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public void batchInsertMyRatedFilm(List<MyRatedFilm> lmrf){
		int i=0;
		Iterator<MyRatedFilm> it=lmrf.iterator();
		MyRatedFilm film=null;
		PreparedStatement stmt;
		try {
			conn.setAutoCommit(false);
			stmt = conn.prepareStatement("INSERT INTO mymovies  VALUES (?, ?)");
			while ( it.hasNext() ){
				film=it.next();						
				stmt.setInt(1, film.getFilmId());
				stmt.setInt(2, film.getMyrate());
				stmt.addBatch();		
				i++;
				if (i == 2000){
					// submit the batch for execution
					stmt.executeBatch();
					i=0;
				}				
			}
			if (i>0){
				stmt.executeBatch();
			}
			conn.commit();
		} catch(SQLException e){
			System.err.println("SQLException: " + e.getMessage());			
			System.err.println("MyRatedFilm:"+film);
			nErrorsDB++;
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	
	public void batchInsertFilmTitle(List<Film> lf){
		int i=0;
		Iterator<Film> it=lf.iterator();
		Film film=null;
		PreparedStatement stmt;
		try {
			conn.setAutoCommit(false);
			stmt = conn.prepareStatement("INSERT INTO movietitle  VALUES (?, ?)");
			while ( it.hasNext() ){
				film=it.next();						
				stmt.setInt(1, film.getFilmId());
				stmt.setString(2, film.getFilmTitle());
				stmt.addBatch();		
				i++;
				if (i == 2000){
					// submit the batch for execution
					stmt.executeBatch();
					i=0;
				}				
			}
			if (i>0){
				stmt.executeBatch();
			}
			conn.commit();
		} catch(SQLException e){
			System.err.println("SQLException: " + e.getMessage());
			System.err.println("FilmTitle:"+film);
			nErrorsDB++;
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	
	public void batchInsertFilm(List<Film> lf){
		int i=0;
		Iterator<Film> it=lf.iterator();
		Film film=null;
		PreparedStatement stmt;
		try {
			conn.setAutoCommit(false);
			stmt = conn.prepareStatement("INSERT INTO movie VALUES (?, ?, ?)");
			while ( it.hasNext() ){
				film=it.next();						
				stmt.setInt(1, film.getFilmId());
				stmt.setFloat(2, film.getAvgRate());
				stmt.setInt(3, film.getRateNumbers());
				stmt.addBatch();		
				i++;
				if (i == 2000){
					// submit the batch for execution
					stmt.executeBatch();
					i=0;
				}				
			}
			if (i>0){
				stmt.executeBatch();
			}
			conn.commit();
		} catch(SQLException e){
			System.err.println("SQLException:" + e.getMessage());
			System.err.println("Film:"+film);
			nErrorsDB++;
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}


	public int getnErrorsDB() {
		return nErrorsDB;
	}
	
	

}
