package w4cash.report;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSaver;

import w4cash.LoadDatabase;

// Stores uploaded .jrxml report templates (+ a compiled .jasper cache alongside) on local
// disk, one directory per machine - mirrors AppConfig's per-machine-config convention rather
// than a DB table, since these are admin-uploaded template files, not application data.
// Filling runs each report's own <queryString> directly against a pooled connection via
// JasperReports' built-in JDBC data source - no custom JRDataSource needed (unlike the legacy
// desktop app's JRDataSourceBasic, which existed only to bridge Openbravo's own SQL abstraction).
@Component
public class ReportsRepository {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private final File reportsDir;

	public ReportsRepository() {
		this(new File(System.getProperty("user.home"), "w4cash-reports"));
	}

	// Package-private, test-only: lets ReportsRepositoryTest point at a temp directory
	// instead of the real per-machine ~/w4cash-reports used in production.
	ReportsRepository(File reportsDir) {
		this.reportsDir = reportsDir;
		reportsDir.mkdirs();
	}

	public List<String> listNames() {
		String[] files = reportsDir.list((dir, name) -> name.toLowerCase().endsWith(".jrxml"));
		List<String> names = new ArrayList<>();
		if (files != null) {
			for (String f : files) {
				names.add(f.substring(0, f.length() - ".jrxml".length()));
			}
		}
		Collections.sort(names);
		return names;
	}

	public boolean exists(String name) {
		return name != null && jrxmlFile(name).isFile();
	}

	public void upload(String name, byte[] jrxmlBytes) throws JRException, IOException {
		File tmp = File.createTempFile("upload-", ".jrxml");
		try {
			Files.write(tmp.toPath(), jrxmlBytes);
			JasperReport report = JasperCompileManager.compileReport(tmp.getAbsolutePath());
			Files.write(jrxmlFile(name).toPath(), jrxmlBytes);
			JRSaver.saveObject(report, jasperFile(name));
		} finally {
			tmp.delete();
		}
	}

	public boolean delete(String name) {
		boolean existed = jrxmlFile(name).isFile();
		jrxmlFile(name).delete();
		jasperFile(name).delete();
		return existed;
	}

	public List<ReportParamInfo> parameters(String name) throws JRException {
		JasperReport report = load(name);
		List<ReportParamInfo> result = new ArrayList<>();
		for (JRParameter p : report.getParameters()) {
			if (!p.isSystemDefined()) {
				result.add(new ReportParamInfo(p.getName(), p.getValueClass().getSimpleName()));
			}
		}
		return result;
	}

	public byte[] run(String name, Map<String, String> rawParams) throws JRException {
		JasperReport report = load(name);
		Map<String, Object> params = convertParams(report, rawParams);
		// Borrow a connection for the fill and hand it back afterwards. While a
		// single connection was shared process-wide, a long report blocked every
		// other request for its whole duration.
		try (Connection conn = LoadDatabase.getConnection()) {
			JasperPrint print = JasperFillManager.fillReport(report, params, conn);
			return JasperExportManager.exportReportToPdf(print);
		} catch (SQLException e) {
			throw new JRException("Could not obtain a database connection for report " + name, e);
		}
	}

	JasperReport load(String name) throws JRException {
		File jasper = jasperFile(name);
		File jrxml = jrxmlFile(name);
		if (jasper.isFile() && jasper.lastModified() >= jrxml.lastModified()) {
			try {
				return (JasperReport) JRLoader.loadObject(jasper);
			} catch (JRException e) {
				// stale/corrupt cache - fall through and recompile from source
			}
		}
		JasperReport report = JasperCompileManager.compileReport(jrxml.getAbsolutePath());
		JRSaver.saveObject(report, jasper);
		return report;
	}

	private Map<String, Object> convertParams(JasperReport report, Map<String, String> rawParams) {
		Map<String, Object> result = new HashMap<>();
		if (rawParams == null) {
			return result;
		}
		Map<String, JRParameter> declared = new HashMap<>();
		for (JRParameter p : report.getParameters()) {
			if (!p.isSystemDefined()) {
				declared.put(p.getName(), p);
			}
		}
		for (Map.Entry<String, String> entry : rawParams.entrySet()) {
			JRParameter decl = declared.get(entry.getKey());
			if (decl == null || entry.getValue() == null) {
				continue;
			}
			result.put(entry.getKey(), convertValue(entry.getValue(), decl.getValueClass()));
		}
		return result;
	}

	private Object convertValue(String raw, Class<?> type) {
		try {
			if (type == String.class) {
				return raw;
			}
			if (type == java.util.Date.class || type == Date.class || type == Timestamp.class) {
				java.util.Date parsed = new SimpleDateFormat(DATE_FORMAT).parse(raw);
				if (type == Date.class) {
					return new Date(parsed.getTime());
				}
				if (type == Timestamp.class) {
					return new Timestamp(parsed.getTime());
				}
				return parsed;
			}
			if (type == Integer.class) {
				return Integer.valueOf(raw);
			}
			if (type == Long.class) {
				return Long.valueOf(raw);
			}
			if (type == Double.class) {
				return Double.valueOf(raw);
			}
			if (type == BigDecimal.class) {
				return new BigDecimal(raw);
			}
			if (type == Boolean.class) {
				return Boolean.valueOf(raw);
			}
		} catch (ParseException | NumberFormatException e) {
			// fall through - pass the raw string; JasperReports will surface a clearer
			// type-mismatch error at fill time than we could produce here
		}
		return raw;
	}

	private File jrxmlFile(String name) {
		return new File(reportsDir, name + ".jrxml");
	}

	private File jasperFile(String name) {
		return new File(reportsDir, name + ".jasper");
	}
}
