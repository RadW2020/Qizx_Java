package jimenez_raul_tp_ad05;

/**
 * ****************************************************************************
 *
 *
 * @author IMCG
 */
//Interfaces y clases para gestionar la BD XML, las colecciones y documentos
import com.qizx.api.Collection;
import com.qizx.api.Configuration;
import com.qizx.api.DataModelException;
import com.qizx.api.Library;
import com.qizx.api.LibraryException;
import com.qizx.api.LibraryManager;
import com.qizx.api.LibraryMember;
import com.qizx.api.QizxException;
//Interfaces y clases para el procesamiento y análisis de documentos XML
import org.xml.sax.SAXException;
//Interfaces y clases para gestionar archvios y flujos de datos
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tp {

    //Valores que se establecen para la creación de la base de datos XML que
    //se creará y los colecciones que contendrá
    //ruta física del Grupo de bases de datos (Library Group)
    private static final String directorioLibraryGroup = "c:\\misdbxml";
    //Nombre de la BD XML (Library): Tutorial
    private static final String bdNombre = "Cursillos";
    //nodo raíz colecciones de la BD XML
    private static final String raiz = "/";
    //ruta de las colecciones origen a importar
    private static final String datosOrigenRuta
            = "C:\\BDCursillosXML\\cursillos_datos\\";

    //array con nombres de las colecciones origen a importar
    private static final String[] datosOrigenNombre
            = {"Aulas", "Cursos", "Profesores"};

    //filtro de extensiones de los ficheros (documentos) a importar: xml y xhtml
    private static final String extensionFiltro = "xml,xhtml";

    /**
     * A partir de colecciones y documentos XML y XHTML en una localización en
     * disco crea la estructura de una BD XML
     *
     * @param args
     */
    public static void main(String[] args) {

        //variables locales
        String nombre;
        // grupo de bibliotecas
        LibraryManager bdManager = null;
        // biblioteca o BD
        Library bd = null;
        //filtro de ficheros o documentos
        FileFilter filtro = new FiltroFichero(extensionFiltro);
        //objeto File apuntando al directorio para el LibraryGropup (grupo de BD XML)
        File directorioGrupo = new File(directorioLibraryGroup);

        try {
            //obtiene el manejador del grupo de BD XML
            bdManager = obtenerBDManager(directorioGrupo);
            //obtiene la  conexión a la BD XML
            bd = obtenerBD(bdManager, bdNombre);
            //crea objeto miembro con la raiz absoluta
            LibraryMember miembroBD = bd.getMember(raiz);
            //comprueba si el miembro es una colección válida
            boolean miembroEsColeccion = (miembroBD
                    != null && miembroBD.isCollection());
            //si no es una colección, cierra la BD bd y el grupo bdManager, y
            //muestra mensaje
            if (!miembroEsColeccion) {
                cerrar(bd, bdManager);
                System.out.println("'" + raiz + "', no existe o no es una colección");
            } else {
                //objeto File apuntando al directorio con datos origen
                File file = new File(datosOrigenRuta);
                // si existe el directorio con datos origen
                if (file.isDirectory()) {
                    //para cada miembro
                    for (String strOrigenNombre : datosOrigenNombre) {
                        //objeto file apuntando a la colección fuente 
                        file = new File(datosOrigenRuta + strOrigenNombre);
                        //si es un miembro colección, guarda en 'nombre' su /nombre
                        if (file.isDirectory()) {
                            if (miembroEsColeccion) {
                                nombre = raiz + strOrigenNombre;
                                //el método llenar() vincula a la base de datos bd, la coleccion
                                //localizada en 'file', denominada 'nombre', conteniendo los
                                //documentos según filtro
                                llenar(bd, file, filtro, nombre);
                            }
                        } else {
                            System.err.println("No existe el directorio con datos origen : "
                                    + strOrigenNombre);
                        }
                    }
                } else {
                    System.err.println("No existe el directorio con datos origen : "
                            + datosOrigenRuta);
                }

                System.out.println("Confirmados cambios...");
                //Confirma las operaciones de la transacción
                bd.commit();
            }

        } catch (IOException ex) {
            Logger.getLogger(Tp.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("ERROR io: " + ex.getMessage());
        } catch (QizxException ex) {
            Logger.getLogger(Tp.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("ERROR Qizx: " + ex.getMessage());
        } catch (SAXException ex) {
            Logger.getLogger(Tp.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("ERROR SAX: " + ex.getMessage());

        } finally {
            //cierra o realiza la desconexión de la BD bd
            cerrar(bd, bdManager);
        }
    }

    /**
     * crea el directorio asociado al grupo de BDs si no existe y devuelve el
     * manejador
     *
     * @param directorioGrupo
     * @return directorio de almacenamiento asociado al grupo de BDs
     * @throws IOException
     * @throws QizxException
     */
    private static LibraryManager obtenerBDManager(File directorioGrupo)
            throws IOException, QizxException {

        //si existe el directorio asociado al grupo, devuelve el manejador
        //LibraryManager asociado
        if (directorioGrupo.exists()) {
            return Configuration.openLibraryGroup(directorioGrupo);
            //si no existe el directorio, intenta crearlo, y devuelve el manejador
            //LibraryManager asociado
        } else {
            if (!directorioGrupo.mkdirs()) {
                throw new IOException("no se puede crear directorio '" + directorioGrupo
                        + "'");
            }
            System.out.println("creando el Grupo de BDs en '"
                    + directorioGrupo + "'...");
            return Configuration.createLibraryGroup(directorioGrupo);
        }
    }

    /**
     * abre la base de datos XML, y si no existe la crea y la abre
     *
     * @param bdManager
     * @param bdNombre
     * @return conexión a la base de datos
     * @throws QizxException
     */
    private static Library obtenerBD(LibraryManager bdManager, String bdNombre)
            throws QizxException {

        //abre una conexión a la BD XML de nombre  bdNombre
        Library bd = bdManager.openLibrary(bdNombre);
        //Si no se ha abierto la BD (porque no existe)
        if (bd == null) {
            System.out.println("Creando BD XML '" + bdNombre + "'...");
            //Crea la BD XML en un subdirectorio del grupo con nombre 'bdNombre'
            bdManager.createLibrary(bdNombre, null);
            //Abre una conexión a la BD creada
            bd = bdManager.openLibrary(bdNombre);
        }
        //devuelve la conexión
        return bd;
    }

    /**
     * crea las colecciones en la base de datos BD XML y llena cada colección
     * con documentos y/o colecciones según filtro
     *
     * @param bd
     * @param fichero
     * @param filtro
     * @param ruta
     * @throws IOException
     * @throws QizxException
     * @throws SAXException
     */
    private static void llenar(Library bd, File fichero, FileFilter filtro,
            String ruta) throws IOException, QizxException, SAXException {

        if (fichero.isDirectory()) { //si es un directorio
            //obtiene la colección de la BD bd situada en esa ruta
            Collection coleccion = bd.getCollection(ruta);
            //si no existe la colección, crea la colección
            if (coleccion == null) {
                System.out.println("Creando colección '" + ruta + "'...");
                coleccion = bd.createCollection(ruta);
            }
            //Guarda en files, los ficheros con extensión coincidente en el filtro
            File[] files = fichero.listFiles(filtro);
            if (files == null) {
                throw new IOException("Error al listar directorio '" + fichero + "'");
            }
            //para cada fichero lo incluye en su correspondiente colección de la BD
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                llenar(bd, file, filtro, ruta + "/" + file.getName());
            }
            //si no es un directorio, lo importa como documento XML analizándolo
        } else {
            System.out.println("Importando '" + fichero + "' como documento '" + ruta
                    + "'...");
            //importa a bd, en la posición indicada por ruta, el documento XML
            // cuyo texto XML está en fichero
            bd.importDocument(ruta, fichero);
        }
    }

    /**
     * cierra la conexión a la base de datos y el grupo
     *
     * @param bd
     * @param bdManager
     */
    private static void cerrar(Library bd, LibraryManager bdManager) {

        try {
            if (bd != null) {
                //si la base de datos está inmersa en una transacción
                if (bd.isModified()) {
                    //deshace los cambios realizados por la transacción
                    bd.rollback();
                }
                //cierra la conexión a la base de datos bd
                bd.close();
            }

            if (bdManager != null) { //cierra las bases de datos del grupo después 
                //de 10000 ms
                bdManager.closeAllLibraries(10000);
            }
        } catch (LibraryException ex) {
            Logger.getLogger(Tp.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("ERROR Library: " + ex.getMessage());
        } catch (DataModelException ex) {
            Logger.getLogger(Tp.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("ERROR DataModel: " + ex.getMessage());
        }

    }
}
