class DetermineLocalityDemo
{
    public boolean isURL(String possibleURL)
    {
        return possibleURL.startsWith("http://")
            || possibleURL.startsWith("https://")
            || possibleURL.contains(":");
    }

    public determineLocalityOfPath(
        String rawPath,
        String baseSiteDirectory,
        String pathOfSourceDoc
    )
    {
        String absolutePath = null /*something rawPath*/;

        if (absolutePath.startsWith(baseSiteDirectory)) {
            if (url.startsWith(pathOfSourceDoc)) {
                return ResourceKind.INTRAPAGE;
            }
            return ResourceKind.INTERNAL;
        }

        return ResourceKind.EXTERNAL;
    }

    public determineLocalityOfURL(
        String rawURL,
        List<String> baseSiteURLs,
        String urlOfSourceDoc
    )
    {
        for (String baseSiteURL : baseSiteURLs) {
            if (rawURL.startsWith(baseSiteURL)) {
                if (url.startsWith(urlOfSourceDoc)) {
                    return ResourceKind.INTRAPAGE;
                }
                return ResourceKind.INTERNAL;
            }
        }

        return ResourceKind.EXTERNAL;
    }

    public determineLocality(
        String extractedURI,
        String baseSiteDirectory,
        List<String> baseSiteURLs,
        String pathOfSourceDoc,
        String urlOfSourceDoc
    )
    {
        if (isURL(rawURI)) {
            return determineLocalityOfUrl(rawURI, baseSiteURLs, urlOfSourceDoc);
        }
        else {
            return determineLocalityOfPath(rawURI, baseSiteDirectory, pathOfSourceDoc);
        }
    }
}
