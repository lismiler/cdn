import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

import java.text.SimpleDateFormat

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */
packageName = ""
typeMapping = [(~/(?i)tinyint|smallint|mediumint/)      : "Integer",
               (~/(?i)bool|bit/)                        : "Boolean",
               (~/(?i)int/)                             : "Long",
               (~/(?i)float|double|decimal|real/)       : "BigDecimal",
               (~/(?i)datetime|timestamp/)              : "Date",
               (~/(?i)date/)                            : "Date",
               (~/(?i)time/)                            : "Date",
               (~/(?i)/)                                : "String",
               (~/(?i)blob|binary|bfile|clob|raw|image/): "InputStream"]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir -> SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

// 获取包所在文件夹路径
def getPackageName(dir) {
    return dir.toString().replaceAll("\\\\", ".").replaceAll("/", ".").replaceAll("^.*src(\\.main\\.java\\.)?", "") + ";"
}


def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    packageName = getPackageName(dir)
    new File(dir, className + "Entity.java").withPrintWriter("utf-8") { out -> generate(out, table, className, fields) }
}


def generate(out, table, className, fields) {

    Set<String> types = new HashSet<String>()
    fields.each() {
        types.add(it.type)
    }

    def tableName = table.getName();
    out.println "package $packageName"
    out.println ""
    out.println "import lombok.Data;"
    out.println "import java.io.Serializable;"
    out.println "import javax.persistence.*;"

    if (types.contains("Date")) {
        out.println "import java.util.Date;"
    }

    if (types.contains("InputStream")) {
        out.println "import java.io.InputStream;"
    }

    if (types.contains("BigDecimal")) {
        out.println "import java.math.BigDecimal;"
    }
    out.println ""
    out.println "/**"
    out.println " * @author  lisongyu "
    out.println " * @date  ${new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())}"
    out.println " */"
    out.println "@Data"
    out.println "@Entity"
    out.println "@Table(name = \"$tableName\")"
    out.println "public class ${className}Entity  implements Serializable{"
    out.println ""
    out.println genSerialID()
    fields.each() {
        out.println ""
        if (it.annos != "") out.println "  ${it.annos}"
        if (isNotEmpty(it.commoent)) {
            out.println "\t/**"
            out.println "\t * ${it.commoent.toString()}"
            out.println "\t */"
        }
        if ("id".equalsIgnoreCase(it.srcName)){
            out.println "\t@Id"
//            out.println "\t@GeneratedValue(strategy = GenerationType.IDENTITY)"
        }
        out.println "\t@Column(name = \"${it.srcName}\")"
        out.println "\tprivate ${it.type} ${it.name};"
    }
    out.println ""
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[name    : javaName(col.getName(), false),
                    srcName : col.getName(),
                    type    : typeStr,
                    commoent: col.getComment(),
                    annos   : ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}


static String genSerialID() {
    return "\tprivate static final long serialVersionUID =  " + Math.abs(new Random().nextLong()) + "L;"
}

def isNotEmpty(content) {
    return content != null && content.toString().trim().length() > 0
}

