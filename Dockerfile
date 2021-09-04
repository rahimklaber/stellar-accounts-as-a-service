FROM openjdk:8-jdk
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/stellar_muxed_wallets/ /app/
WORKDIR /app/bin
CMD ["./stellar_muxed_wallets"]
