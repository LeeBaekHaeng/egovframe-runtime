package org.egovframe.rte.fdl.excel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * FileServiceTest is TestCase of File Handling Service
 * @author Seongjong Yoon
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:META-INF/spring/context-*.xml" })
@Transactional
public class EgovExcelUploadTest extends AbstractJUnit4SpringContextTests {

	protected static String usingDBMS = "hsql";

	@Resource(name = "jdbcProperties")
	protected Properties jdbcProperties;

	@Resource(name = "dataSource")
	protected DataSource dataSource;

	@Resource(name = "excelService")
	private EgovExcelService excelService;

	@Resource(name = "excelBigService")
	private EgovExcelService excelBigService;

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovExcelUploadTest.class);

	@Before
	public void onSetUp() throws Exception {
		ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource("META-INF/testdata/sample_schema_ddl_" + usingDBMS + ".sql"));
	}

	@Rollback(false)
	@Test
	public void testUploadExcelFile() throws Exception {

		try {
			LOGGER.debug("testUploadExcelFile start....");

			FileInputStream fileIn = new FileInputStream(new File("testdata/testBatch.xls"));
			excelService.uploadExcel("insertEmpUsingBatch", fileIn);

		} catch (Exception e) {
			LOGGER.error(e.toString());
			throw new Exception(e);
		} finally {
			LOGGER.debug("testUploadExcelFile end....");
		}
	}

	/**
	 * 대용량 엑셀파일 업로드
	 * @throws Exception
	 */
	@Rollback(false)
	@Test
	public void testBigUploadExcelFile() throws Exception {

		try {
			LOGGER.debug("testBigUploadExcelFile start....");

			FileInputStream fileIn = new FileInputStream(new File("testdata/zipExcel.xls"));
			excelBigService.uploadExcel("insertZipUsingBatch", fileIn, 2, (long) 5000);

		} catch (Exception e) {
			LOGGER.error("Exception - Runtime.getRuntime().freeMemory() : {}", Runtime.getRuntime().freeMemory());
			LOGGER.error(e.toString());
			throw new Exception(e);
		} finally {
			LOGGER.debug("testBigUploadExcelFile end....");
		}
	}

}