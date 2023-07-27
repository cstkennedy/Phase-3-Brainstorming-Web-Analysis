---
title: Program Flow - Web Analysis
toc: yes
author: Thomas J. Kennedy
---


# Program Flow

In total... there are three distinct phases into which WebAnalysis program
logic can be divided:

  1. Data Analysis and Extraction
  2. Data Normalization (e.g., path and URL normalization)
  3. Report Generation


## Data Analysis and Extraction

Let us examine the actual analysis. *Note that we will focus on the broad
strokes.* Details such as `Exception`s, page count checks, path
validation/checks, and URL validation will not be included.

```plantuml
@startuml sequence_01.svg
autoactivate on
hide footbox
skinparam backgroundColor #FFFFFF
skinparam sequenceParticipant underline

title Data Analysis and Extraction: High-Level View

participant ":WebAnalysis" as driver
activate driver

create ":WebsiteBuilder" as wb

driver -> wb: new()
return

driver -> wb: withPath(path)
return

driver -> wb: withURLs(urls)
return

driver -> wb: build()
    wb -> wb: walkDirectory()
    return files
    wb -> wb: removeNonHTMLFiles(files)
    return prunedFiles

    create "site: Website" as site
    wb -> site: new(path, URLs)
    return

    loop for htmlFile in prunedFiles
        create ":BufferedReader" as buffer
        wb -> buffer: new(htmlFile)
        return
        create ":HTMLDocumentBuilder" as hb
        wb -> hb: new()
        return
        wb -> hb: withContentFrom(buffer)
        return
        wb -> hb: extractContent()
            hb -> hb: extractAnchors()
            return
            hb -> hb: extractImages()
            return
            hb -> hb: extractScripts()
            return
            hb -> hb: extractStyleSheets()
            return
        return
        wb -> hb: build()
            create "doc:HTMLDocument" as doc
            hb -> doc: new() [creation details not listed]
            return
        return doc 

        wb -> buffer !!: delete
        wb -> hb !!: delete

        wb -> site:addPage(doc)
        return
    end

return site

driver -> wb !!: delete

@enduml
```

![](sequence_01.svg)

**Note that we did not cover the creation details of `HTMLDocument` objects
within the `HTMLDocumentBuilder.build` method.** I will leave this part up to
you. Take note of how

  1. all `HTMLDocumentBuilder` objects are destroyed
  2. `WebsiteBuilder` is destroyed
  3. all `BufferedReader` objects are destroyed

We have populated the `site` (`Website` object) with all the `HTMLDocument`
objects. The analysis objects are no longer needed.


### Creation & Deletion

Take note of the `new` and `delete` function "calls."

  1. `new` represents a call to a constructor (i.e., creating a new object)

  2. `delete` represents an object being "deallocated" (i.e., leaving scope and
     being discarded)

     *Even though Java is a Garbage Collected language wherein memory
     management is not handled explicitly by a programmer... designing around
     (and making use of) object lifetimes is **still** important.* **(Even if
     some teachers are cavalier in their approach.)**


## Further Analysis

I will leave the intermediary steps (i.e., all steps until report generation)
up to you and your team.


## Report Generation

Since the *Builder* objects have been destroyed... we no longer need to
consider them.


```plantuml
@startuml sequence_03.svg
autoactivate on
hide footbox
skinparam backgroundColor #FFFFFF
skinparam sequenceParticipant underline

title Report Generation: High-Level View

participant "site:Website" as site
participant ":WebAnalysis" as driver
activate driver


create ":ReportManager" as manager

driver -> manager: new()
return
driver -> manager: setSourceData(site)
return

driver -> manager: determineBaseFileName()
return

driver -> manager: writeAll()
    create ":TextReportWriter" as textWriter
    manager -> textWriter: new()
    return
    manager -> textWriter: setSourceData(site)
    return
    manager -> textWriter: setBaseName(...)
    return
    manager -> textWriter: write()
    return
    manager -> textWriter !!: delete

    create ":JSONReportWriter" as jsonWriter
    manager -> jsonWriter: new()
    return
    manager -> jsonWriter: setSourceData(site)
    return
    manager -> jsonWriter: setBaseName(...)
    return
    manager -> jsonWriter: write()
    return
    manager -> jsonWriter !!: delete

    create ":ExcelReportWriter" as excelWriter
    manager -> excelWriter: new()
    return
    manager -> excelWriter: setSourceData(site)
    return
    manager -> excelWriter: setBaseName(...)
    return
    manager -> excelWriter: write()
    return
    manager -> excelWriter !!: delete
return

driver -> manager: writeReportNames(...)
return

@enduml
```

![](sequence_03.svg)


## Generating the Reports - Implementation

Working with text files can be accomplished with the Java `BufferedWriter` and
`FileWriter` classes. The other reports require external libraries. In total
(including HTML parsing) the following libraries should be utilized...


| Organization      | Artifact ID   | Version   | Gradle Dependency                | Description                                                                      |
| :------------:    | :-----------: | --------- | --------------                   | --------------------------                                                       |
| **org.jsoup**     | jsoup         | 1.16.1    | org.jsoup:jsoup:1.16.1           | [HTML Parsing](https://jsoup.org/)                                               |
| org.apache.poi    | poi           | 5.2.3+    | org.apache.poi:poi:5.2.3+        | [Read/write Microsoft document formats](https://poi.apache.org/)                 |
| org.apache.poi    | poi-ooxml     | 5.2.3+    | org.apache.poi:poi-ooxml:5.2.3+  | [Read & write Excel spreadsheets](https://poi.apache.org/spreadsheet/index.html) |
| com.cedarsoftware | json-io       | 4.14.0    | com.cedarsoftware:json-io:4.14.0 | [JSON Export/Import](https://github.com/jdereg/json-io)                          |

The table should be familiar... it is listed on the [Design Notes
page](https://www.cs.odu.edu/~tkennedy/cs350/latest/Protected/websiteAnalysisDesignNotes)
for the Semester Project. The *Gradle Dependency* column even lists the
specific dependency for use in `build.gradle`

I recommend adding...

```
    implementation 'org.jsoup:jsoup:1.16.1'
    implementation 'org.apache.poi:poi:5.2.3+'
    implementation 'org.apache.poi:poi-ooxml:5.2.3+'
    implementation 'com.cedarsoftware:json-io:4.14.0'
```

to the dependency block within your team's `build.gradle` file.

