/*
 * Copyright 2008-2014 MOPAS(Ministry of Public Administration and Security).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.egovframe.rte.fdl.excel.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;
import org.egovframe.rte.fdl.excel.EgovExcelMapping;
import org.egovframe.rte.fdl.excel.EgovExcelService;
import org.egovframe.rte.fdl.filehandling.EgovFileUtil;
import org.egovframe.rte.fdl.string.EgovObjectUtil;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * 엑셀 서비스를 처리하는 비즈니스 구현 클래스.
 * 
 * <p>
 * <b>NOTE:</b> 엑셀 서비스를 제공하기 위해 구현한 클래스이다.
 * </p>
 * 
 * @author 실행환경 개발팀 윤성종
 * @since 2009.06.01
 * @version 1.0
 * 
 *          <pre>
 * 개정이력(Modification Information)
 *
 * 수정일		수정자				수정내용
 * ----------------------------------------------
 *   2009.06.01  윤성종          최초 생성
 *   2013.05.22  이기하          XSSF, SXSSF형식 추가(xlsx 지원)
 *   2013.05.29  한성곤          mapBeanName property 추가 및 코드 정리
 *   2014.05.14  이기하          코드 refactoring 및 mybatis 서비스 추가
 *   2017.02.15  장동한          ES-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
 *   2020.08.31  ESFC          ES-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
 *   2024.08.17  이백행          시큐어코딩 Exception 제거
 *          </pre>
 */
public class EgovExcelServiceImpl implements EgovExcelService, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovExcelServiceImpl.class);

	private MessageSource messageSource;
	private ApplicationContext applicationContext;
	private String mapClass;
	private String mapBeanName;
	private EgovExcelServiceDAO dao;
	private EgovExcelServiceMapper excelBatchMapper;
	private SqlMapClient sqlMapClient;
	private SqlSessionTemplate sqlSessionTemplate;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.messageSource = (MessageSource) applicationContext.getBean("messageSource");
	}

	/**
	 * MessageSource 얻기.
	 * 
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * ibatis 적용시 설정.
	 * 
	 * @param sqlMapClient
	 */
	public void setSqlMapClient(SqlMapClient sqlMapClient) {
		this.sqlMapClient = sqlMapClient;
		dao = new EgovExcelServiceDAO(this.sqlMapClient);
	}

	/**
	 * mybatis 적용시 설정.
	 * 
	 * @param sqlSessionTemplate
	 */
	public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
		excelBatchMapper = new EgovExcelServiceMapper(this.sqlSessionTemplate);
	}

	/**
	 * Excel Cell과 VO를 mapping 구현 클래스.
	 * 
	 * @param mapClass
	 */
	public void setMapClass(String mapClass) {
		this.mapClass = mapClass;
		LOGGER.debug("mapClass : {}", mapClass);
	}

	/**
	 * Excel Cell과 VO를 mapping 구현 Bean name (mapClass보다 우선함).
	 * 
	 * @param mapBeanName
	 */
	public void setMapBeanName(String mapBeanName) {
		this.mapBeanName = mapBeanName;
		LOGGER.debug("mapBeanName : {}", mapBeanName);
	}

	/**
	 * Workbook 객체를 생성하여 엑셀파일을 생성한다.
	 * 
	 * @param wb
	 * @param filepath
	 * @return Workbook
	 */
	@Override
	public Workbook createWorkbook(Workbook wb, String filepath) {
		try {
			String fullFileName = filepath;
			LOGGER.debug("EgovExcelServiceImpl.createWorkbook 1 : templatePath is {}",
					FilenameUtils.getFullPath(fullFileName));
			if (!EgovFileUtil.isExistsFile(FilenameUtils.getFullPath(fullFileName))) {
				LOGGER.debug("make dir {}", FilenameUtils.getFullPath(fullFileName));
				FileUtils.forceMkdir(new File(FilenameUtils.getFullPath(fullFileName)));
			}
			FileOutputStream fileOut = null;
			LOGGER.debug("EgovExcelServiceImpl.createWorkbook 2 : templatePath is {}", fullFileName);
			try {
				LOGGER.debug("ExcelServiceImpl filepath ...");
				fileOut = new FileOutputStream(fullFileName);
				wb.write(fileOut);
			} finally {
				LOGGER.debug("ExcelServiceImpl loadExcelObject end...");
				if (wb != null)
					wb.close();
				;
				if (fileOut != null)
					fileOut.close();
			}
			return wb;
		} catch (IOException e) {
			throw new BaseRuntimeException("IOException: createWorkbook", e);
		}
	}

	/**
	 * 엑셀 Template를 로딩하여 엑셀파일을 생성한다.
	 * 
	 * @param templateName
	 * @return Workbook
	 */
	@Override
	public Workbook loadExcelTemplate(String templateName) {
		try {
			FileInputStream fileIn = null;
			Workbook wb = null;
			LOGGER.debug("EgovExcelServiceImpl.loadExcelTemplate : templatePath is {}", templateName);
			try {
				LOGGER.debug("ExcelServiceImpl loadExcelTemplate ...");
				fileIn = new FileInputStream(templateName);
				wb = new HSSFWorkbook(fileIn);
			} finally {
				LOGGER.debug("ExcelServiceImpl loadExcelTemplate end...");
				if (wb != null)
					wb.close();
				if (fileIn != null)
					fileIn.close();
			}
			return wb;
		} catch (IOException e) {
			throw new BaseRuntimeException("IOException: loadExcelTemplate(String templateName)", e);
		}
	}

	/**
	 * xlsx 엑셀 Template를 로딩하여 엑셀파일을 생성한다.
	 * 
	 * @param templateName
	 * @param wb
	 * @return Workbook
	 */
	@Override
	public XSSFWorkbook loadExcelTemplate(String templateName, XSSFWorkbook wb) {
		try {
			FileInputStream fileIn = null;
			LOGGER.debug("EgovExcelServiceImpl.loadExcelTemplate(XSSF) : templatePath is {}", templateName);
			try {
				LOGGER.debug("ExcelServiceImpl loadExcelTemplate(XSSF) ...");
				fileIn = new FileInputStream(templateName);
				wb = new XSSFWorkbook(fileIn);
			} finally {
				LOGGER.debug("ExcelServiceImpl loadExcelTemplate(XSSF) end...");
				if (wb != null)
					wb.close();
				if (fileIn != null)
					fileIn.close();
			}
			return wb;
		} catch (IOException e) {
			throw new BaseRuntimeException("IOException: loadExcelTemplate(String templateName, XSSFWorkbook wb)", e);
		}
	}

	/**
	 * 엑셀 파일을 로딩한다.
	 * 
	 * @param filepath
	 * @return Workbook
	 */
	@Override
	public Workbook loadWorkbook(String filepath) {
		try {
			FileInputStream fileIn = null;
			Workbook wb = null;
			try {
				fileIn = new FileInputStream(filepath);
				wb = loadWorkbook(fileIn);
			} finally {
				if (wb != null)
					wb.close();
				if (fileIn != null)
					fileIn.close();
			}
			return wb;
		} catch (IOException e) {
			throw new BaseRuntimeException("IOException: loadWorkbook(String filepath)", e);
		}
	}

	/**
	 * xlsx 엑셀 파일을 로딩한다.
	 * 
	 * @param filepath
	 * @param wb
	 * @return XSSFWorkbook
	 */
	@Override
	public XSSFWorkbook loadWorkbook(String filepath, XSSFWorkbook wb) {
		try {
			FileInputStream fileIn = null;
			try {
				fileIn = new FileInputStream(filepath);
				wb = loadWorkbook(fileIn, wb);
			} finally {
				if (wb != null)
					wb.close();
				if (fileIn != null)
					fileIn.close();
			}
			return wb;
		} catch (IOException e) {
			throw new BaseRuntimeException("IOException: loadWorkbook(String filepath, XSSFWorkbook wb)", e);
		}
	}

	/**
	 * 엑셀 파일을 로딩한다.
	 * 
	 * @param fileIn
	 * @return
	 */
	@Override
	public Workbook loadWorkbook(InputStream fileIn) {
		try {
			POIFSFileSystem fs = null;
			Workbook wb = null;
			try {
				LOGGER.debug("ExcelServiceImpl loadWorkbook ...");
				fs = new POIFSFileSystem(fileIn);
				wb = new HSSFWorkbook(fs);
			} finally {
				if (wb != null)
					wb.close();
				if (fs != null)
					fs.close();
				if (fileIn != null)
					fileIn.close();
			}
			return wb;
		} catch (IOException e) {
			throw new BaseRuntimeException("IOException: loadWorkbook(InputStream fileIn)", e);
		}
	}

	/**
	 * xlsx 엑셀 파일을 로딩한다.
	 * 
	 * @param fileIn
	 * @param wb
	 * @return
	 */
	@Override
	public XSSFWorkbook loadWorkbook(InputStream fileIn, XSSFWorkbook wb) {
		try {
			LOGGER.debug("ExcelServiceImpl loadWorkbook(XSSF) ...");
			wb = new XSSFWorkbook(fileIn);
			return wb;
		} catch (IOException e) {
			throw new BaseRuntimeException("IOException: loadWorkbook(InputStream fileIn, XSSFWorkbook wb)", e);
		}
	}

	/**
	 * 엑셀파일을 업로드하여 DB에 일괄저장한다.<br/>
	 * 업로드할 엑셀의 시작 위치를 정하여 지정한 셀부터 업로드한다.<br/>
	 * commit할 건수를 입력한다.<br/>
	 * sqlSessionTemplate(mybatis), sqlMapClient(ibatis)로 구분한다.
	 * 
	 * @param queryId
	 * @param sheet
	 * @param start     (default : 0)
	 * @param commitCnt (default :0)
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, Sheet sheet, int start, long commitCnt) {
		LOGGER.debug("sheet.getPhysicalNumberOfRows() : {}", sheet.getPhysicalNumberOfRows());
		Integer rowsAffected = 0;
		long rowCnt = sheet.getPhysicalNumberOfRows();
		long cnt = (commitCnt == 0) ? rowCnt : commitCnt;
		LOGGER.debug("Runtime.getRuntime().totalMemory() : {}", Runtime.getRuntime().totalMemory());
		LOGGER.debug("Runtime.getRuntime().freeMemory() : {}", Runtime.getRuntime().freeMemory());
		long startTime = System.currentTimeMillis();
		for (int idx = start, i = start; idx < rowCnt; idx = i) {
			List<Object> list = new ArrayList<Object>();
			LOGGER.debug("before Runtime.getRuntime().freeMemory() : {}", Runtime.getRuntime().freeMemory());
			EgovExcelMapping mapping = null;
			if (mapBeanName != null) {
				mapping = (EgovExcelMapping) applicationContext.getBean(mapBeanName);
			} else if (mapClass != null) {
				mapping = (EgovExcelMapping) EgovObjectUtil.instantiate(mapClass);
			} else {
				throw new RuntimeException(
						getMessageSource().getMessage("error.excel.property.error", null, Locale.getDefault()));
			}
			for (i = idx; i < rowCnt && i < (cnt + idx); i++) {
				Row row = sheet.getRow(i);
				list.add(mapping.mappingColumn(row));
			}
			if (sqlSessionTemplate != null) {
				rowsAffected += excelBatchMapper.batchInsert(queryId, list);
			} else if (sqlMapClient != null) {
				rowsAffected += dao.batchInsert(queryId, list);
			} else {
				throw new RuntimeException(
						getMessageSource().getMessage("error.excel.persistence.error", null, Locale.getDefault()));
			}
			LOGGER.debug("after Runtime.getRuntime().freeMemory() : {}", Runtime.getRuntime().freeMemory());
			LOGGER.debug("rowsAffected : {}", rowsAffected);
		}
		LOGGER.debug("batchInsert time is {}", (System.currentTimeMillis() - startTime));
		LOGGER.debug("uploadExcel result count is {}", rowsAffected);
		return rowsAffected;
	}

	/**
	 * 엑셀파일을 읽어서 DB upload 한다.
	 * 
	 * @param queryId
	 * @param fileIn
	 * @param start(default     : 0)
	 * @param commitCnt(default : 0)
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, InputStream fileIn, int start, long commitCnt) {
		Workbook wb = loadWorkbook(fileIn);
		Sheet sheet = wb.getSheetAt(0);
		return uploadExcel(queryId, sheet, start, commitCnt);
	}

	/**
	 * xlsx 엑셀파일을 읽어서 DB upload 한다.
	 * 
	 * @param queryId
	 * @param fileIn
	 * @param start
	 * @param commitCnt
	 * @param wb
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, InputStream fileIn, int start, long commitCnt, XSSFWorkbook wb) {
		wb = loadWorkbook(fileIn, wb);
		Sheet sheet = wb.getSheetAt(0);
		return uploadExcel(queryId, sheet, start, commitCnt);
	}

	/**
	 * 엑셀파일을 저장한다.
	 * 
	 * @param queryId
	 * @param fileIn
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, InputStream fileIn) {
		return uploadExcel(queryId, fileIn, 0, 0);
	}

	/**
	 * xlsx 엑셀파일을 저장한다.
	 * 
	 * @param queryId
	 * @param fileIn
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, InputStream fileIn, XSSFWorkbook type) {
		return uploadExcel(queryId, fileIn, 0, 0, type);
	}

	/**
	 * 엑셀파일을 업로드하여 DB에 일괄저장한다.<br/>
	 * 업로드할 엑셀의 시작 위치를 정하여 지정한 셀부터 업로드한다.
	 * 
	 * @param queryId
	 * @param fileIn
	 * @param sheetIndex
	 * @param start      (default : 0)
	 * @param commitCnt  (default : 0)
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, InputStream fileIn, short sheetIndex, int start, long commitCnt) {
		Workbook wb = loadWorkbook(fileIn);
		Sheet sheet = wb.getSheetAt(sheetIndex);
		return uploadExcel(queryId, sheet, start, commitCnt);
	}

	/**
	 * xlsx 엑셀파일을 업로드하여 DB에 일괄저장한다.<br/>
	 * 업로드할 엑셀의 시작 위치를 정하여 지정한 셀부터 업로드한다.
	 * 
	 * @param queryId
	 * @param fileIn
	 * @param sheetIndex
	 * @param start      (default : 0)
	 * @param commitCnt  (default : 0)
	 * @param wb
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, InputStream fileIn, short sheetIndex, int start, long commitCnt,
			XSSFWorkbook wb) {
		wb = loadWorkbook(fileIn, wb);
		Sheet sheet = wb.getSheetAt(sheetIndex);
		return uploadExcel(queryId, sheet, start, commitCnt);
	}

	/**
	 * 엑셀파일을 업로드하여 DB에 일괄저장한다.<br/>
	 * 업로드할 엑셀의 시작 위치를 정하여 지정한 셀부터 업로드한다.
	 * 
	 * @param queryId
	 * @param fileIn
	 * @param sheetName
	 * @param start     (default : 0)
	 * @param commitCnt (default : 0)
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, InputStream fileIn, String sheetName, int start, long commitCnt) {
		Workbook wb = loadWorkbook(fileIn);
		Sheet sheet = wb.getSheet(sheetName);
		return uploadExcel(queryId, sheet, start, commitCnt);
	}

	/**
	 * xlsx 엑셀파일을 업로드하여 DB에 일괄저장한다.<br/>
	 * 업로드할 엑셀의 시작 위치를 정하여 지정한 셀부터 업로드한다.
	 * 
	 * @param queryId
	 * @param fileIn
	 * @param sheetName
	 * @param start     (default : 0)
	 * @param commitCnt (default : 0)
	 * @param wb
	 * @return
	 */
	@Override
	public Integer uploadExcel(String queryId, InputStream fileIn, String sheetName, int start, long commitCnt,
			XSSFWorkbook wb) {
		wb = loadWorkbook(fileIn, wb);
		Sheet sheet = wb.getSheet(sheetName);
		return uploadExcel(queryId, sheet, start, commitCnt);
	}

}
