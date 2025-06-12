import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
public class laboratorio2 {
    public static void main(String[] args) {
        try {
            IloCplex model = new IloCplex();

            // Datos de entrada
            int nTareas = 5;
            int nCPUs = 3;
            double[] rt = {261.27, 560.89, 310.51, 105.80, 344.7};  // Recursos solicitados por cada tarea
            double[] rc = {505.67, 503.68, 701.78};  // Capacidad de cada CPU

            // Variables de decisión x_tc[t][c] (binarias)
            IloNumVar[][] x_tc = new IloNumVar[nTareas][nCPUs];
            for (int t = 0; t < nTareas; t++) {
                for (int c = 0; c < nCPUs; c++) {
                    x_tc[t][c] = model.boolVar("x_" + (t + 1) + "_" + (c + 1));
                }
            }

            // Variable z (carga máxima) que se quiere minimizar
            IloNumVar z = model.numVar(0, Double.MAX_VALUE, "z");
            model.addMinimize(z);  // Objetivo: Minimizar la carga máxima

            // Restricción 1: Asignar cada tarea a exactamente una CPU
            for (int t = 0; t < nTareas; t++) {
                IloLinearNumExpr sumadeAsignaciones = model.linearNumExpr();
                for (int c = 0; c < nCPUs; c++) {
                    sumadeAsignaciones.addTerm(1.0, x_tc[t][c]);
                }
                model.addEq(sumadeAsignaciones, 1, "Asignacion de tarea " + (t + 1));
            }

            // Restricción 2: La carga de cada CPU no puede exceder su capacidad
            for (int c = 0; c < nCPUs; c++) {
                IloLinearNumExpr cargaCPU = model.linearNumExpr();
                for (int t = 0; t < nTareas; t++) {
                    cargaCPU.addTerm(rt[t], x_tc[t][c]);
                }
                model.addLe(cargaCPU, rc[c], "Carga CPU: " + (c + 1));
            }

            // Restricción 3: La carga máxima (z) debe ser al menos la carga en cada CPU
            for (int c = 0; c < nCPUs; c++) {
                IloLinearNumExpr maxLoad = model.linearNumExpr();
                for (int t = 0; t < nTareas; t++) {
                    maxLoad.addTerm(rt[t], x_tc[t][c]);
                }
                model.addGe(z, maxLoad, "Z_Upperbound " + (c + 1));
            }

            // Resolver el modelo
            if (model.solve()) {
                System.out.println("--- Solución encontrada ---");
                System.out.println("Valor óptimo de z: " + model.getObjValue());

                // Asignación de tareas
                System.out.println("\nAsignación de tareas:");
                for (int t = 0; t < nTareas; t++) {
                    for (int c = 0; c < nCPUs; c++) {
                        if (model.getValue(x_tc[t][c]) > 0.5) {  // Si la tarea está asignada a la CPU
                            System.out.println("Tarea: " + (t + 1) + " está asignada al CPU: " + (c + 1));
                        }
                    }
                }
                // Cargas de los CPUs
                System.out.println("\nCargas del CPU:");
                for (int c = 0; c < nCPUs; c++) {
                    double carga = 0;
                    for (int t = 0; t < nTareas; t++) {
                        carga += rt[t] * model.getValue(x_tc[t][c]);
                    }
                    double porcentajeDeUtilizacion = (carga / rc[c]) * 100;
                    System.out.println("CPU: " + (c + 1) + " está cargada al: " + String.format("%.2f", porcentajeDeUtilizacion) + "%");
                }
            } else {
                System.out.println("Solución no encontrada.");
            }

            model.end();  // Liberar recursos del modelo

        } catch (IloException e) {
            throw new RuntimeException(e);  // Manejo de excepciones
        }
    }
}