package modeles;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class BSim_2004 {

	public static void solve(Données _data, int limit_deviation, boolean identical_deviation) {
		try {

			int n = _data.Nbr_Items ;
			int W = _data.Size_Bin ;

			// Majorant sur le nombre de Bin
			int M = _data.Nbr_Items;

			// définir un nouveau model 
			IloCplex model = new IloCplex();

			// variables
			IloIntVar[][] x = new IloIntVar[n][M];
			IloIntVar[] y = new IloIntVar[M];

			IloNumVar[] pi = new IloNumVar[M] ;
			IloNumVar[][] mu = new IloNumVar[n][M];

			for (int j=0; j<M;j++) {
				for(int i=0;i<n;i++) {
					x[i][j] = model.boolVar("x["+i+"]["+j+"]");
					mu[i][j] = model.numVar(0, Double.MAX_VALUE , "mu["+i+"]["+j+"]" ) ;
					model.add(x[i][j]) ;
					model.add(mu[i][j]) ;
				}
				y[j] = model.boolVar("y["+j+"]");
				pi[j] = model.numVar(0, Double.MAX_VALUE , "pi["+j+"]" ) ;
				model.add(y[j]) ;
				model.add(pi[j]);
			}


			// objectif
			IloLinearIntExpr obj = model.linearIntExpr() ;
			for(int j=0; j<M; j++) {
				obj.addTerm(1, y[j]);
			}
			model.addMinimize(obj) ;

			// contrainte d'affectation 	
			for(int i=0; i<n ; i++) {
				IloLinearIntExpr expr = model.linearIntExpr();
				for(int j=0; j<M; j++) {
					expr.addTerm(1, x[i][j]);
				}
				model.addEq(expr, 1);
			}


			IloLinearNumExpr expr = model.linearNumExpr(); // pour ne pas en creer une à chaque fois 

			// contrainte de sac a dos 
			for(int j=0; j<M; j++) {
				for(int i=0; i<n; i++) {
					expr.addTerm(_data.Items_Sizes.get(i)+_data.Deviations.get(i), x[i][j]);
					expr.addTerm(1, mu[i][j]);
				}
				expr.addTerm(limit_deviation, pi[j]);
				expr.addTerm( -W  , y[j]);
				model.addLe(expr,0) ;
				expr.clear();
			}

			//expr.clear();
			// contrainte des duales 
			if( ! identical_deviation ) { // differentes parties incertaines
				for(int j=0; j<M; j++) {
					for(int i=0; i<n ; i++) {
						expr.addTerm(-_data.Deviations.get(i),x[i][j]);
						expr.addTerm(1, pi[j]);
						expr.addTerm(1, mu[i][j]);
						model.addGe(expr,0) ;
						expr.clear();
					}
				}
			}
			else { // parties incertaines identiques pour tout les items
				for(int j=0; j<M; j++) {
					for(int i=0; i<n ; i++) {
						expr.addTerm(-_data.Deviations.get(0),x[i][j]);
						expr.addTerm(1, pi[j]);
						expr.addTerm(1, mu[i][j]);
						model.addGe(expr,0) ;
						expr.clear();
					}
				}
			}


			//model.exportModel("model1.lp") ;
			
			
			
			model.setParam(IloCplex.Param.TimeLimit	, 60); 

			if (model.solve()) {
				for(int i=0; i<n;i++) {
					for(int j=0; j<M ; j++) {
						if( model.getValue(x[i][j]) != 0) {	
							System.out.println("x["+i+"]["+j+"] = " + model.getValue(x[i][j]));						
						}		
					}
				}
				System.out.println("le nombre de contraintes est: " + model.getNrows() );
				//model.writeSolutions("solution.lp") ;
				System.out.println("\n Le nombre de Bin est = "+model.getObjValue() + "\n");
			}
			else {
				System.out.println( model.getMIPRelativeGap() );
				System.out.println("problem not solved");
			}

			// close model
			model.close();

		} catch (IloException exp) {
			exp.printStackTrace();
		}
	}
}
