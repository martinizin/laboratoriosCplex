import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.concert.IloObjective; // Import IloObjective

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

            // NUEVA: Variables para manejar el exceso de capacidad (overload)
            IloNumVar[] overload_c = new IloNumVar[nCPUs];
            for (int c = 0; c < nCPUs; c++) {
                // overload_c[c] can be 0 or positive
                overload_c[c] = model.numVar(0, Double.MAX_VALUE, "overload_CPU_" + (c + 1));
            }

            // MODIFICACIÓN DEL OBJETIVO: Minimizar la carga máxima Y el exceso de capacidad
            IloLinearNumExpr objectiveExpr = model.linearNumExpr();
            objectiveExpr.addTerm(1.0, z); // Main objective: minimize z
            double penalty_coefficient = 1000.0; // High penalty for overload
            for (int c = 0; c < nCPUs; c++) {
                objectiveExpr.addTerm(penalty_coefficient, overload_c[c]);
            }
            model.addMinimize(objectiveExpr);


            // Restricción 1: Asignar cada tarea a exactamente una CPU (mantener esta)
            for (int t = 0; t < nTareas; t++) {
                IloLinearNumExpr sumadeAsignaciones = model.linearNumExpr();
                for (int c = 0; c < nCPUs; c++) {
                    sumadeAsignaciones.addTerm(1.0, x_tc[t][c]);
                }
                model.addEq(sumadeAsignaciones, 1, "Asignacion de tarea " + (t + 1));
            }

            // MODIFICACIÓN DE RESTRICCIÓN 2: La carga de cada CPU no puede exceder su capacidad
            // PERO permitiendo un 'overload' que es penalizado
            for (int c = 0; c < nCPUs; c++) {
                IloLinearNumExpr cargaCPU = model.linearNumExpr();
                for (int t = 0; t < nTareas; t++) {
                    cargaCPU.addTerm(rt[t], x_tc[t][c]);
                }
                // cargaCPU - overload_c[c] <= rc[c]  => cargaCPU <= rc[c] + overload_c[c]
                model.addLe(cargaCPU, model.sum(rc[c], overload_c[c]), "Carga CPU: " + (c + 1));
            }

            // Restricción 3: La carga máxima (z) debe ser al menos la carga en cada CPU
            // Note: If z is intended to be the *actual* max load, it should be based on cargaCPU
            // without considering the 'overload_c' variable in this constraint, as that's already handled above.
            for (int c = 0; c < nCPUs; c++) {
                IloLinearNumExpr currentLoad = model.linearNumExpr();
                for (int t = 0; t < nTareas; t++) {
                    currentLoad.addTerm(rt[t], x_tc[t][c]);
                }
                model.addGe(z, currentLoad, "Z_Upperbound " + (c + 1));
            }

            // Resolver el modelo
            if (model.solve()) {
                System.out.println("--- Solución encontrada ---");
                System.out.println("Valor óptimo de la función objetivo (z + penalidad por sobrecarga): " + model.getObjValue());
                System.out.println("Valor de z (carga máxima real en una CPU): " + model.getValue(z));

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
                    double actualOverload = model.getValue(overload_c[c]);

                    System.out.println("CPU: " + (c + 1) + " (Capacidad: " + rc[c] + ") está cargada con: " + String.format("%.2f", carga) + " (" + String.format("%.2f", porcentajeDeUtilizacion) + "%)");
                    if (actualOverload > 0.001) { // Check if overload is significant
                        System.out.println("  *** SOBRECARGA DETECTADA: " + String.format("%.2f", actualOverload) + " unidades ***");
                    }
                }
            } else {
                System.out.println("Solución no encontrada. Esto no debería ocurrir con las restricciones relajadas.");
            }

            model.end();  // Liberar recursos del modelo

        } catch (IloException e) {
            throw new RuntimeException(e);  // Manejo de excepciones
        }
    }
}