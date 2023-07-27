---
title: A Little More than Pseudocode - Web Analysis
toc: yes
author: Thomas J. Kennedy
---

# Overview

Let us start with the `main` function. We know from the rules of *Top-Down
Design* that the

> *main function should do no work*

However, there is a bit of a corollary...

> *...other than calling functions and maintaining a few variables...*

And a bit more...

> *and maybe some basic command line argument validation*

```java
public class Driver
{
    public static void main(String[] args)
    {
        // Handle user arguments
        String websitePath = args[0];

        // Grab the remaining arguments using a Java Stream
        // (for some functional style programming)
        List<String> urls = Arrays.stream(args)
            .skip(1)
            .collect()
            .toList();

        Website site = new WebsiteBuilder()
            .withPath(websitePath)
            .withURLs(urls)
            .build();

        ReportManager manager = new ReportManager();
        manager.setSourceData(site);

        // We want to control when this happens... since time does not pause.
        manager.determineBaseFilename();

        // Write the reports before writing the filenames.
        // If something goes wrong... we do not want to
        // output the filename for a report that was not generated
        manager.writeAll();

        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(System.out)
        );
        manager.writeReportNames(writer);
    }
}
```

Did you notice how our design takes care of the main function? Of course...
there is some exception handling left to add. I will leave that as an exercise
to the reader (you and your team).


# WebsiteBuilder & HTMLDocumentBuilder

There is quite a bit in `WebsiteBuilder` and `HTMLDocumentBuilder`. However,
our focus is on the extraction logic. You will find a few new helper methods
(and maybe utility functions/classes).

## WebsiteBuilder

```java
public class WebsiteBuilder
{
    private Path path;
    private List<URL> urls;

    public WebsiteBuilder()
    {
        //...
    }

    //...
    // Implement the various "with" methods
    //...

    //...
    // Implement walkDirectory
    //...

    //...
    // Implement removeNonHTMLFiles
    //...

    public Website build()
        throws /*Various Exceptions*/
    {
        List<Path> files = walkDirectory();
        List<Path> prunedFiles = pruneNonHTMLFiles(files);


        List<HTMLDocument> parsedDocuments = new ArrayList<>();
        for (Path htmlFile : prunedFiles) {
            BufferedReader buffer = new BufferedReader(/*...htmlFile...*/);

            HTMLDocument doc = new HTMLDocumentBuilder()
                .withContentFrom(buffer)
                .withWebsiteBaseDir(this.path)  // needed for path normalization
                .withWebsiteURLs(this.urls)  // needed for internal/external classification
                .extractContent()  // exceptions can be thrown by this function
                .build();

            parsedDocuments.add(doc);
        }

        Website site = new Website(this.path, this.urls, parsedDocuments);

        return website;
    }

}
```

Take note of what the *Builder Pattern* gives us. It guarantees that when we
create a `Website` object, we already have all the data (particularly
`HTMLDocument` objects) ready to go.


## HTMLDocumentBuilder

To implement `HTMLDocumentBuilder`, I will assume that
[`SimpleHTMLParser`](https://github.com/cstkennedy/cs350-examples/tree/master/Office-Hours/OfficeHours-JSoup)
is utilized for all HTML tag extraction operations.

```java
public class HTMLDocumentBuilder
{
    private List<Resource> anchors;
    private List<Resource> images;
    private List<Resource> scripts;
    private List<Resource> stylesheets;

    private List<URL> baseUrls;
    private Path baseDirectory;

    private BufferedReader readBuffer;

    public HTMLDocumentBuilder()
    {
        this.anchors = new ArrayList<>();
        this.images = new ArrayList<>();
        this.scripts = new ArrayList<>();
        this.stylesheets = new ArrayList<>();

        //...
        //...
        //...
    }

    //...
    // Implement withContentFrom (both variants)
    //...

    //...
    // Implement withBaseDirectory
    //...

    //...
    // Implement withBaseURLs
    //...

    List<Resource> extractAnchors()
        throws IOException, FileNotFoundException
    {
        SimpleHTMLParser parser = new SimpleHTMLParser("a", "href");
        List<String> extractedStrings = parser.extractAllURIs(this.readBuffer);

        // The URIs (URLs and Paths) are currently in string form.
        // As part of the analysis, they need to be converted to Resource objects

        //...

        this.anchors = /*...*/

        return this.anchors;
    }

    List<Resource> extractImages()
        throws IOException, FileNotFoundException
    {
        SimpleHTMLParser parser = new SimpleHTMLParser("img", "src");
        List<String> extractedStrings = parser.extractAllURIs(this.readBuffer);

        // The URIs (URLs and Paths) are currently in string form.
        // As part of the analysis, they need to be converted to Resource objects

        //...

        this.images = /*...*/

        return this.images;
    }

    List<Resource> extractScripts()
        throws IOException, FileNotFoundException
    {
        SimpleHTMLParser parser = new SimpleHTMLParser("script", "src");
        List<String> extractedStrings = parser.extractAllURIs(this.readBuffer);

        // The URIs (URLs and Paths) are currently in string form.
        // As part of the analysis, they need to be converted to Resource objects

        //...

        this.scripts = /*...*/

        return this.scripts;
    }

    List<Resource> extractStyleSheets()
        throws IOException, FileNotFoundException
    {
        SimpleHTMLParser parser = new SimpleHTMLParser("link", "href");
        List<String> extractedStrings = parser.extractAllURIs(this.readBuffer);

        // The URIs (URLs and Paths) are currently in string form.
        // As part of the analysis, they need to be converted to Resource objects

        //...

        this.stylesheets = /*...*/

        return this.stylesheets;
    }

    public void extractContent()
        throws IOException, FileNotFoundException
    {
        this.extractAnchors();
        this.extractImages();
        this.extractScripts();
        this.extractStyleSheets();
    }

    //...
    // Implement build
    //...
```

The various `extract` methods are similar to each other. Barring the
*intrapage* classification for anchors and the different tag attribute
combinations... the four functions implement the same foundational logic.

Let us look at `extractImages` again.

```java
    List<Resource> extractImages()
        throws IOException, FileNotFoundException
    {
        SimpleHTMLParser parser = new SimpleHTMLParser("img", "src");
        List<String> extractedStrings = parser.extractAllURIs(this.readBuffer);

        // The URIs (URLs and Paths) are currently in string form.
        // As part of the analysis, they need to be converted to Resource objects

        //...

        this.images = /*...*/

        return this.images;
    }
```

We need logic to determine if the URI is

  1. a `Path` or a `URL`
  2. internal or external

This would also be the time to think about computing file size, considering
path/URL normalization, and handling boundary checks for relative paths.

Let us start by adding a little more detail to `extractImages`...

```java
    List<Resource> extractImages()
        throws IOException, FileNotFoundException
    {
        SimpleHTMLParser parser = new SimpleHTMLParser("img", "src");
        List<String> extractedStrings = parser.extractAllURIs(this.readBuffer);

        // The URIs (URLs and Paths) are currently in string form.
        // As part of the analysis, they need to be converted to Resource objects

        for (String uriAsString : extractedStrings) {
            ResourceKind type = ResourceKind.IMAGE; 

            Locality location = this.determineLocality(uriAsString, this.baseSiteURLs);

            Resource image = new Image();

            // Setting the ResourceKind should be handled automatically by
            // the Image Constructors
            image.setKind(type);
            image.setLocation(location);

            // We know that the only two cases are "internal" and "external"
            if (location == Locality.EXTERNAL) {
                image.setURL(/*converted uriAsString*/);
                image.setPath(null);
            }
            else {
                image.setURL(null);

                String pathAsString = this.convertURLToPath(uriAsString, this.baseSiteURLs);
                image.setPath(/*converted pathAsString*/);

                long fileSizeInKiB = this.determineFileSize(uriAsString);
                image.setSize(fileSizeInKiB);
            }
            this.images.add(image);
        }

        return this.images;
    }
```

There is quite a bit happening here. We introduced:

  - `determineFileSize`
  - `determineLocality`
  - `convertURLToPath`
  - various `Resource` setters

Since this is *Java* and not Rust... I would probably introduce a
`ResourceBuilder` (with a little `ResourceFactory` logic).

A lot of my design takes inspiration from functional programmming (specifically
the notion of pure functions). You can see a lot of that with the design we
have discussed, e.g., differing creation of an object until we have every piece
of data **and** have handled all exceptions.

Now... the `Resource` setters are not too interesting. Note how for:

  - internal images `setURL` uses `null` as the argument
  - external images `setPath` uses `null` as the argument

If the image is internal... we only care about the path. The reverse is true
for external images (where the notion of a Path does not make sense).

The three new functions (methods in this case)

  - `determineFileSize`
  - `determineLocality`
  - `convertURLToPath`

should really (in my opinion) be part of a `ResourceBuilder` class.


## ResourceBuilder?

Take a moment to revisit the `extractImage` loop...

What would happen if we introduced `ResourceBuilder`?

```java
        for (String uriAsString : extractedStrings) {
            Resource image = new ResourceBuilder()
                .withType(ResourceKind.IMAGE)
                .withURI(/*uriAsString*/)
                .usingURLContext(this.baseSiteURLs)
                .usingSiteRootContext(this.baseSiteDirectory)
                .determineLocality() // uriAsString was already supplied
                .determineFileSizeIfLocal()
                .normalizePathAndURL() // baseSiteDirectory was already supplied
                .build();

            this.images.add(image);
        }
```

All the analysis logic for a `Resource` is now wrapped up in a neat package. I
could justify either approach. However, the Builder Pattern does result in more
readable (and testable) code.


# ReportManager

The `ReportManager` is primarily a convenience class. It creates all three
`ReportWriter`s, handles passing them the data, and then forwards any
`Exception`s to the calling code (`main` in our case).

```java
public class ReportManager
{
    private String baseFilename;
    private Website site;
    
    public ReportManager()
    {
        this.baseFilename = null;
        this.site = null;
    }

    public setSourceData(Website sourceData)
    {
        this.site = sourceData;
    }

    public void determineBaseFileName()
    {
        // Datetime logic...

        this.baseFileName = /*Set based on datetime logic*/;
    }

    public void writeReportNames(BufferedWriter nameWriter)
        throws IOException
    {
        String reportName = String.format("%s.txt", this.baseFilename);
        nameWriter.write(reportName);

        reportName = String.format("%s.json", this.baseFilename);
        nameWriter.write(reportName);

        reportName = String.format("%s.xlsx", this.baseFilename);
        nameWriter.write(reportName);

        nameWriter.flush();
    }

    public void writeAll()
        throws /*Various Exceptions*/
    {
        ReportWriter writer = null;
        
        writer = new TextReportWriter();
        writer.setSourceData(this.site);
        writer.setBaseName(this.baseFilename);
        writer.write();

        writer = new JSONReportWriter();
        writer.setSourceData(this.site);
        writer.setBaseName(this.baseFilename);
        writer.write();

        writer = new ExcelReportWriter();
        writer.setSourceData(this.site);
        writer.setBaseName(this.baseFilename);
        writer.write();
    }
```

I will leave the actual `ReportWriter` classes up to you.


