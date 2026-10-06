package com.example.sync

object GoogleAppsScriptTemplate {
    const val SCRIPT_CODE = """/**
 * Google Apps Script for GSC Student Manager Attendance Sync
 * 
 * Instructions:
 * 1. Open your Google Sheet.
 * 2. Click Extensions > Apps Script.
 * 3. Replace all code in Code.gs with this script.
 * 4. Click Deploy > New Deployment.
 * 5. Select type "Web app".
 * 6. Set "Execute as": "Me".
 * 7. Set "Who has access": "Anyone" (so the Android app can push and pull data).
 * 8. Click Deploy and copy the Web App URL into GSC Student Manager!
 */

function doPost(e) {
  try {
    var sheetApp = SpreadsheetApp.getActiveSpreadsheet();
    if (!e || !e.postData || !e.postData.contents) {
      return ContentService.createTextOutput(JSON.stringify({ status: "error", message: "No data received" }))
        .setMimeType(ContentService.MimeType.JSON);
    }
    
    var data = JSON.parse(e.postData.contents);

    // If client requested data pull via POST
    if (data.action === "GET_DATA" || data.action === "GET_STUDENTS") {
      return getSheetDataJson(sheetApp);
    }

    var attendanceList = data.attendance || [];
    var studentsList = data.students || [];
    
    // 1. Update or create "Students" tab
    var studentSheet = sheetApp.getSheetByName("Students");
    if (!studentSheet) {
      studentSheet = sheetApp.insertSheet("Students");
      studentSheet.appendRow(["Roll No", "Student Name", "Room No", "Phone", "Internal ID"]);
      studentSheet.getRange("A1:E1").setFontWeight("bold").setBackground("#047857").setFontColor("#FFFFFF");
    }
    
    if (data.students !== undefined) {
      studentSheet.clearContents();
      studentSheet.appendRow(["Roll No", "Student Name", "Room No", "Phone", "Internal ID"]);
      studentSheet.getRange("A1:E1").setFontWeight("bold").setBackground("#047857").setFontColor("#FFFFFF");
      if (studentsList.length > 0) {
        var sRows = studentsList.map(function(s) {
          return [s.rollNo, s.name, s.roomNo, s.phone, s.id];
        });
        studentSheet.getRange(2, 1, sRows.length, 5).setValues(sRows);
      }
    }
    
    // 2. Update or create "Attendance Log" tab
    var logSheet = sheetApp.getSheetByName("Attendance Log");
    if (!logSheet) {
      logSheet = sheetApp.insertSheet("Attendance Log");
      logSheet.appendRow(["Date", "Activity / Meal", "Roll No", "Student Name", "Room No", "Status", "Recorded At"]);
      logSheet.getRange("A1:G1").setFontWeight("bold").setBackground("#065F46").setFontColor("#FFFFFF");
    }
    
    if (data.attendance !== undefined) {
      logSheet.clearContents();
      logSheet.appendRow(["Date", "Activity / Meal", "Roll No", "Student Name", "Room No", "Status", "Recorded At"]);
      logSheet.getRange("A1:G1").setFontWeight("bold").setBackground("#065F46").setFontColor("#FFFFFF");
      if (attendanceList.length > 0) {
        var aRows = attendanceList.map(function(a) {
          var recordedDate = a.recordedAt ? new Date(a.recordedAt).toLocaleString() : "";
          return [a.date, a.mealType, a.studentRollNo, a.studentName, a.roomNo, a.status, recordedDate];
        });
        logSheet.getRange(2, 1, aRows.length, 7).setValues(aRows);
      }
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      message: "Synced " + attendanceList.length + " attendance records and " + studentsList.length + " students to Google Sheet.",
      updatedRows: attendanceList.length,
      studentCount: studentsList.length
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  try {
    var sheetApp = SpreadsheetApp.getActiveSpreadsheet();
    return getSheetDataJson(sheetApp);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function getSheetDataJson(sheetApp) {
  var students = [];
  var studentSheet = sheetApp.getSheetByName("Students");
  if (!studentSheet && sheetApp.getSheets().length > 0) {
    studentSheet = sheetApp.getSheets()[0];
  }
  
  if (studentSheet) {
    var sValues = studentSheet.getDataRange().getValues();
    if (sValues.length > 1) {
      // Find header column indices
      var headers = sValues[0].map(function(h) { return String(h).trim().toLowerCase(); });
      var rollCol = headers.findIndex(function(h) { return h.indexOf("roll") !== -1 || h.indexOf("id") !== -1; });
      var nameCol = headers.findIndex(function(h) { return h.indexOf("name") !== -1 || h.indexOf("student") !== -1; });
      var roomCol = headers.findIndex(function(h) { return h.indexOf("room") !== -1 || h.indexOf("hostel") !== -1; });
      var phoneCol = headers.findIndex(function(h) { return h.indexOf("phone") !== -1 || h.indexOf("mobile") !== -1 || h.indexOf("contact") !== -1; });
      
      if (rollCol === -1) rollCol = 0;
      if (nameCol === -1) nameCol = 1;
      if (roomCol === -1) roomCol = 2;
      if (phoneCol === -1) phoneCol = 3;

      for (var i = 1; i < sValues.length; i++) {
        var row = sValues[i];
        var roll = String(row[rollCol] !== undefined ? row[rollCol] : "").trim();
        var name = String(row[nameCol] !== undefined ? row[nameCol] : "").trim();
        var room = String(row[roomCol] !== undefined ? row[roomCol] : "").trim();
        var phone = String(row[phoneCol] !== undefined ? row[phoneCol] : "").trim();

        if (name !== "" || roll !== "") {
          students.push({
            rollNo: roll || ("10" + i),
            name: name || ("Student " + roll),
            roomNo: room,
            phone: phone
          });
        }
      }
    }
  }

  var attendance = [];
  var logSheet = sheetApp.getSheetByName("Attendance Log");
  if (logSheet) {
    var aValues = logSheet.getDataRange().getValues();
    for (var j = 1; j < aValues.length; j++) {
      var aRow = aValues[j];
      if (aRow.length >= 6) {
        attendance.push({
          date: String(aRow[0]),
          mealType: String(aRow[1]),
          studentRollNo: String(aRow[2]),
          studentName: String(aRow[3]),
          roomNo: String(aRow[4]),
          status: String(aRow[5]),
          recordedAt: aRow[6] ? new Date(aRow[6]).getTime() : new Date().getTime()
        });
      }
    }
  }

  return ContentService.createTextOutput(JSON.stringify({
    status: "success",
    students: students,
    attendance: attendance,
    message: "Fetched " + students.length + " students from Google Sheet"
  })).setMimeType(ContentService.MimeType.JSON);
}
"""
}

