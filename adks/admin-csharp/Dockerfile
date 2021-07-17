FROM mcr.microsoft.com/dotnet/sdk:3.1 AS build-env
ARG Version
WORKDIR /src
ADD . /src
RUN dotnet restore IO.FeatureHub.MR.sln
RUN dotnet publish  -c Release -o out
RUN dotnet build /p:PackageVersion=$Version -c Release --no-restore && \
     dotnet pack /p:PackageVersion=$Version -c Release --no-restore --no-build -o /sln/artifacts
ENTRYPOINT ["dotnet", "nuget", "push", "/sln/artifacts/*.nupkg"]
CMD ["--source", "https://api.nuget.org/v3/index.json"]

# build with  docker build --build-arg Version=2.1.0 -t featurehub-sdk-csharp .
# examine with: docker run --rm -it --entrypoint "/bin/sh" featurehub-sdk-csharp -c /bin/bash
# deploy with docker run --rm  featurehub-sdk-csharp --source https://api.nuget.org/v3/index.json --api-key MY-SECRET-KEY

