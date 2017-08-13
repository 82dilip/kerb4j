package com.kerb4j.pac;

import com.kerb4j.Kerb4JException;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.crypto.CheckSumHandler;
import org.apache.kerby.kerberos.kerb.type.base.CheckSumType;
import org.apache.kerby.kerberos.kerb.type.base.KeyUsage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.Key;
import java.util.Arrays;

public class Pac {

    private PacLogonInfo logonInfo;
    private PacSignature serverSignature;
    private PacSignature kdcSignature;

    public Pac(byte[] data, Key key) throws Kerb4JException
    {
        byte[] checksumData = data.clone();
        try
        {
            PacDataInputStream pacStream = new PacDataInputStream(new DataInputStream(
                    new ByteArrayInputStream(data)));

            if (data.length <= 8)
                throw new Kerb4JException("pac.token.empty", null, null);

            int bufferCount = pacStream.readInt();
            int version = pacStream.readInt();

            if (version != PacConstants.PAC_VERSION)
            {
                Object[] args = new Object[]{version};
                throw new Kerb4JException("pac.version.invalid", args, null);
            }

            for (int bufferIndex = 0; bufferIndex < bufferCount; bufferIndex++)
            {
                final int sigTypeLength = 4;

                int bufferType = pacStream.readInt();
                int bufferSize = pacStream.readInt();
                long bufferOffset = pacStream.readLong();
                byte[] bufferData = new byte[bufferSize];
                System.arraycopy(data, (int) bufferOffset, bufferData, 0, bufferSize);

                switch (bufferType)
                {
                    case PacConstants.LOGON_INFO:
                        // PAC Credential Information
                        logonInfo = new PacLogonInfo(bufferData);
                        break;
                    case PacConstants.SERVER_CHECKSUM:
                        // PAC Server Signature
                        serverSignature = new PacSignature(bufferData);
                        // Clear signature from checksum copy
                        for (int i = 0; i < bufferSize - sigTypeLength; i++)
                            checksumData[(int) bufferOffset + sigTypeLength + i] = 0;
                        break;
                    case PacConstants.PRIVSVR_CHECKSUM:
                        // PAC KDC Signature
                        kdcSignature = new PacSignature(bufferData);
                        // Clear signature from checksum copy
                        for (int i = 0; i < bufferSize - sigTypeLength; i++)
                            checksumData[(int) bufferOffset + sigTypeLength + i] = 0;
                        break;
                    default:
                }
            }
        }
        catch (IOException e)
        {
            throw new Kerb4JException("pac.token.malformed", null, e);
        }

        byte[] checksum;

        try
        {
            checksum = CheckSumHandler.getCheckSumHandler(CheckSumType.fromValue(serverSignature.getType())).
                    checksumWithKey(checksumData, key.getEncoded(), KeyUsage.APP_DATA_CKSUM.getValue());
        }
        catch (KrbException e)
        {
            throw new Kerb4JException("pac.check.fail", null, e);
        }

        if (!Arrays.equals(serverSignature.getChecksum(), checksum))
            throw new Kerb4JException("pac.signature.invalid", null, null);
    }

    public PacLogonInfo getLogonInfo()
    {
        return logonInfo;
    }

    public PacSignature getServerSignature()
    {
        return serverSignature;
    }

    public PacSignature getKdcSignature()
    {
        return kdcSignature;
    }
}
