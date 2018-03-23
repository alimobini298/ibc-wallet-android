package io.awallet.crypto.alphawallet;

import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.SalesOrderMalformed;
import io.awallet.crypto.alphawallet.entity.Wallet;

import static io.awallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Date;
import android.util.Base64;

/**
 * Created by weiwu on 9/3/18.
 */

/**
 * Universal link format
 *
 * Android requires the link to be in the form:
 *
 * https://www.awallet.io/import?
 *
 * The format forbids using a prefix other than 'www'.
 * There needs to be text in the specific link too, in this case 'import'.
 *
 * AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkAKcAqA==;
 * 1b;
 * 2F982B84C635967A9B6306ED5789A7C1919164171E37DCCDF4B59BE547544105;
 * 30818B896B7D240F56C59EBDF209062EE54DA7A3590905739674DCFDCECF3E9B
 *
 * Base64 message: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkAKcAqA==
 * bytes32: price Wei
 * bytes32: expiry
 * bytes20: contract address
 * Uint16[]: ticket indices
 *
 * byte: 1b
 * bytes32: 2F982B84C635967A9B6306ED5789A7C1919164171E37DCCDF4B59BE547544105
 * bytes32: 30818B896B7D240F56C59EBDF209062EE54DA7A3590905739674DCFDCECF3E9B
 *
 */

public class UniversalLinkTest {
    final String link = "https://app.awallet.io/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABvBbWdOyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAALyaECakvG8LqLvkhtHQnaVzKznkALMAtA==;1b;2981CF5F9C45E9957BE897ED2EC749A8CE16086942A241BCDA4E870259B53EF4;2EFBA4BEBC7E3AE4475F4D92BADC1DD4D14D95187CD7403F701AED48CA23737B";

    final String[] links = { "https://app.awallet.io/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWNFeF2KAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAtnMrrswHk+OKmJNHmavTx9w88xANMA1ADVANYA1wDYANkA2g==;1b;4C42984466C576E191687684A48674698510F750413A3F1F0B7F718BCBB37072;4EDF6FA41DAB5C3E6E7B468B2FE384E86B2FD93A9BF5F771C0AAA07C649106EB",
            "https://app.awallet.io/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWNFeF2KAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAtnMrrswHk+OKmJNHmavTx9w88xANsA3ADdAN4=;1c;36F33C72C0B191A0C2587F36E5F77CF5A9243D34010119DDDD98390E6FF12014;1173094E702F64EA3233B9FE515AD7516B8A128E47E9D88BE1F5CC50A5E59295",
            "https://app.awallet.io/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWNFeF2KAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAtnMrrswHk+OKmJNHmavTx9w88xAN8=;1b;623AFBCD4B439A738EBA820B0C1AEDB50FB23155ACABF735B72213E7D935D9E6;42F4EE7C87B63E72FE24C2A9CF323588331E02E5043CC3B91EBA20F53C6842E3" };

    final int[] indices         = new int[] { 0xb3, 0xb4 };
    final String OWNER_ADDR     = "0x007bee82bdd9e866b2bd114780a47f2261c684e3";
    final String CONTRACT_ADDR  = "0xbc9a1026a4bc6f0ba8bbe486d1d09da5732b39e4";
    final String ethPrice       = "0.5";
    final long expiry           = 0;

    @Test
    public void UniversalLinkShouldBeParsedCorrectly() throws SalesOrderMalformed, SignatureException {
        SalesOrder order = SalesOrder.parseUniversalLink(link);
        BigDecimal testPriceWei = Convert.toWei(ethPrice, Convert.Unit.ETHER);
        BigInteger testPriceWeiBi = BigInteger.valueOf(testPriceWei.longValue());

        assertEquals(testPriceWeiBi, order.priceWei);
        assertEquals(expiry, order.expiry);
        assertEquals(CONTRACT_ADDR, order.contractAddress.toLowerCase());
        assertArrayEquals(indices, order.tickets);
        assertTrue(verifySignature(order.message, order.signature));
        Sign.SignatureData signature = sigFromByteArray(order.signature);

        BigInteger pubkey = new BigInteger("3766624743362555291863022291641419798817556312446913485076900228931550311167262358936119031908256138233623094427893806146688851885551327681125435090087130", 10);
        assertEquals(pubkey, Sign.signedMessageToKey(order.message, signature));
    }

    @Test
    public void UniversalLinksTest() throws SalesOrderMalformed, SignatureException {
        for (String link : links) {
            SalesOrder order = SalesOrder.parseUniversalLink(link);
            order.getOwnerKey();
            Sign.SignatureData signature = sigFromByteArray(order.signature);
            assertNotNull(order.contractAddress);
            assertNotNull(order.ownerAddress);
            assertEquals(OWNER_ADDR, order.ownerAddress.toLowerCase()); //created from 0x007
        }
    }

    @Test(expected = SalesOrderMalformed.class)
    public void BadLinksShouldThrow() throws SalesOrderMalformed {
        String link = "https://www.awallet.io/import?bad";
        SalesOrder order = SalesOrder.parseUniversalLink(link);
    }

    //TODO: Once we start generating the link fill this test in
    @Test
    public void UniversalLinkShouldBeGeneratedCorrectly() {
        //SalesOrder order = SalesOrder(......)
    }

    private String ecRecoverAddress(byte[] data, Sign.SignatureData signature) //get the hex string address from the sig and data
    {
        String address = "";
        try
        {
            BigInteger recoveredKey = Sign.signedMessageToKey(data, signature); //get embedded address
            address = Keys.getAddress(recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return address;
    }

    private boolean verifySignature(byte[] message, byte[] signature) {
        boolean pass = false;
        try {
            Sign.SignatureData sig = sigFromByteArray(signature);
            String address = ecRecoverAddress(message, sig);

            if (Numeric.cleanHexPrefix(address).equalsIgnoreCase(Numeric.cleanHexPrefix(OWNER_ADDR)))
            {
                pass = true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return pass;
    }
}
