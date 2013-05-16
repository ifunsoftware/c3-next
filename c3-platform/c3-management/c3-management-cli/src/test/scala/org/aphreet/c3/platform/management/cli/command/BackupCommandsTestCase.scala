package org.aphreet.c3.platform.management.cli.command

import junit.framework.TestCase
import junit.framework.Assert._
import org.easymock.EasyMock._
import org.aphreet.c3.platform.management.cli.command.impl.CreateRemoteTargetCommand
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService

class BackupCommandsTestCase extends TestCase{

  def testCreateRemoteBackup(){

    val management = createMock(classOf[PlatformManagementService])
    expect(management.createRemoteBackupTarget("linuxbox0", "localhost", "root", "/data/backups",
      """-----BEGIN RSA PRIVATE KEY-----
        |MIIEogIBAAKCAQEAw/NjODvHw0a+ledSYUbTAf5gywau1gK/OXH94ucr8DeGwm2c
        |dVQZADMNb5Omd1y90WsGJuhxUGZ2DQTUAP/9y1E2oZi5RnYZoonLSp5ZRtcA52OT
        |f+1QGaSwAGZyPpGPc0iD6pxNEFFrHpmim53gW9tit6QkdjtYU0DVQx7DNMaze8A9
        |vUHzLsR2Tu1L0L1U2PK7ff+A0zWLfOdcPNgwB+KUbt5ssxYwq14hpiZkOK9rXFcT
        |CKrHNK76vSQ89Po6tuzK00Sb/k6dYHuQk9fREzhg1AP20t20FEvOzLbq0gudt3dh
        |HdminWHxoaZ+zF66rW7ceoFVA2TWzVxHXgw9BQIDAQABAoIBAHjo2sVdJdNZ+MnY
        |td1ubU9inmlIk2NcyI/yxb1X9aXBLXcVWaCQJukXl2fMzMAESuBI0L/7V8hLoPZB
        |j2uaigkF8NbfvRs8Tm8b5YQCl4X9rwIYUHB0h6N7Glr02/O9r61sBnIixe6Wvqlg
        |FZU+Yq1rfstgDluiHMsqSy2icsNB8AJITnLEGrRGCW/fqYFom7Ga1x2csNjMkeun
        |owbT7gZrixQvrrlUCyc0l+REpfrDuenJ29rxKL144lFNlyKUvQxSZ3Q3D37L0Crl
        |I7y/QtbbO08rcgIT2yiUA/1aXnF11INbtJ+7xnsENIjR4iVw3msGjjKU+3/bXeqa
        |pqg/H2ECgYEA5qTybcm7YrSB4EnipmGGqQ885tXYOU3s2gdSgs29/cCfD6zQZAad
        |l9WALb+jovyDLy/G9v/OS6XsfynCh78BwPVEdgRnFnygF+p1oh/q/PLwIHkAxUaF
        |28sTHJvcqNBu/Zx21L8QShBOglllPQJBp1KTkcaaX43lUYgb27EdWrkCgYEA2X4N
        |6rj72nKE/rojYi2ywUW6aNBDzUMybjcOw7riomPEOKXalhdYLORgz3M9voXhhJeM
        |2f7w9SM+lm2402MonyAK8xewOnwFMVfHwwA3QLtXcy5FF9lsqHtDXaAYuO69IgAS
        |GlxxEl2rKuF7iTb5IB4vX1VCfcgOKU6kfeoWXq0CgYAjIhcnsCYSKArsnnSfAZYg
        |pIiTZRm1yO/+WkUwVaTORYw1OA7aXcOdwFDxJxMHHc3h3zZAo8DJ9zFqQXM3eFoN
        |CY9vJsaJP1ynp/hZROFnvJ3lZGlAo7h6PZngrsFTGcT+btoPGDekAI3k/zcTrwdP
        |NJ30A7u6OIQpAkG0GmutSQKBgA90bqa8SGFIe/zh2zvRxX8IZmumSPsgCEherzTn
        |Zc1mS34/At5hgEmIXPzA0qVkPBdA8ahX/mVdAMiNuXGD/VH5Y+3MRCCTyYatFyvb
        |KxH1fgxYQF6me5spTysrAja5+ESfPqoS4ZINh+k/Jpkkh+VpX7Jli5kGi0MSLIwq
        |AzbtAoGAUxvrSe+8RFiO9K0ONTxjsE5mYDOhdKbKwzxIKfBOL9/DD+RpJJrcQ1kb
        |/BuSox8VLx6EzcATA+l1LBdD/g7ZQomD3TVP3g4n5VeyA+aAH5t4XMD5OcP2bYr+
        |jN0psKU++xlLUQ6MXoBRYR7nTeyNHU2M9Dz4zVCBTiTlFN0XR1g=
        |-----END RSA PRIVATE KEY-----
        |""".stripMargin))

    replay(management)

    val command = new CreateRemoteTargetCommand

    assertEquals("Remote target created", command.execute(List("linuxbox0", "localhost", "root", "/data/backups", "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFb2dJQkFBS0NBUUVBdy9Oak9Edkh3MGErbGVkU1lVYlRBZjVneXdhdTFnSy9PWEg5NHVjcjhEZUd3bTJjCmRWUVpBRE1OYjVPbWQxeTkwV3NHSnVoeFVHWjJEUVRVQVAvOXkxRTJvWmk1Um5ZWm9vbkxTcDVaUnRjQTUyT1QKZisxUUdhU3dBR1p5UHBHUGMwaUQ2cHhORUZGckhwbWltNTNnVzl0aXQ2UWtkanRZVTBEVlF4N0ROTWF6ZThBOQp2VUh6THNSMlR1MUwwTDFVMlBLN2ZmK0EweldMZk9kY1BOZ3dCK0tVYnQ1c3N4WXdxMTRocGlaa09LOXJYRmNUCkNLckhOSzc2dlNRODlQbzZ0dXpLMDBTYi9rNmRZSHVRazlmUkV6aGcxQVAyMHQyMEZFdk96TGJxMGd1ZHQzZGgKSGRtaW5XSHhvYVorekY2NnJXN2Nlb0ZWQTJUV3pWeEhYZ3c5QlFJREFRQUJBb0lCQUhqbzJzVmRKZE5aK01uWQp0ZDF1YlU5aW5tbElrMk5jeUkveXhiMVg5YVhCTFhjVldhQ1FKdWtYbDJmTXpNQUVTdUJJMEwvN1Y4aExvUFpCCmoydWFpZ2tGOE5iZnZSczhUbThiNVlRQ2w0WDlyd0lZVUhCMGg2TjdHbHIwMi9POXI2MXNCbklpeGU2V3ZxbGcKRlpVK1lxMXJmc3RnRGx1aUhNc3FTeTJpY3NOQjhBSklUbkxFR3JSR0NXL2ZxWUZvbTdHYTF4MmNzTmpNa2V1bgpvd2JUN2dacml4UXZycmxVQ3ljMGwrUkVwZnJEdWVuSjI5cnhLTDE0NGxGTmx5S1V2UXhTWjNRM0QzN0wwQ3JsCkk3eS9RdGJiTzA4cmNnSVQyeWlVQS8xYVhuRjExSU5idEorN3huc0VOSWpSNGlWdzNtc0dqaktVKzMvYlhlcWEKcHFnL0gyRUNnWUVBNXFUeWJjbTdZclNCNEVuaXBtR0dxUTg4NXRYWU9VM3MyZ2RTZ3MyOS9jQ2ZENnpRWkFhZApsOVdBTGIram92eURMeS9HOXYvT1M2WHNmeW5DaDc4QndQVkVkZ1JuRm55Z0YrcDFvaC9xL1BMd0lIa0F4VWFGCjI4c1RISnZjcU5CdS9aeDIxTDhRU2hCT2dsbGxQUUpCcDFLVGtjYWFYNDNsVVlnYjI3RWRXcmtDZ1lFQTJYNE4KNnJqNzJuS0Uvcm9qWWkyeXdVVzZhTkJEelVNeWJqY093N3Jpb21QRU9LWGFsaGRZTE9SZ3ozTTl2b1hoaEplTQoyZjd3OVNNK2xtMjQwMk1vbnlBSzh4ZXdPbndGTVZmSHd3QTNRTHRYY3k1RkY5bHNxSHREWGFBWXVPNjlJZ0FTCkdseHhFbDJyS3VGN2lUYjVJQjR2WDFWQ2ZjZ09LVTZrZmVvV1hxMENnWUFqSWhjbnNDWVNLQXJzbm5TZkFaWWcKcElpVFpSbTF5Ty8rV2tVd1ZhVE9SWXcxT0E3YVhjT2R3RkR4SnhNSEhjM2gzelpBbzhESjl6RnFRWE0zZUZvTgpDWTl2SnNhSlAxeW5wL2haUk9GbnZKM2xaR2xBbzdoNlBabmdyc0ZUR2NUK2J0b1BHRGVrQUkzay96Y1Ryd2RQCk5KMzBBN3U2T0lRcEFrRzBHbXV0U1FLQmdBOTBicWE4U0dGSWUvemgyenZSeFg4SVptdW1TUHNnQ0VoZXJ6VG4KWmMxbVMzNC9BdDVoZ0VtSVhQekEwcVZrUEJkQThhaFgvbVZkQU1pTnVYR0QvVkg1WSszTVJDQ1R5WWF0Rnl2YgpLeEgxZmd4WVFGNm1lNXNwVHlzckFqYTUrRVNmUHFvUzRaSU5oK2svSnBra2grVnBYN0psaTVrR2kwTVNMSXdxCkF6YnRBb0dBVXh2clNlKzhSRmlPOUswT05UeGpzRTVtWURPaGRLYkt3enhJS2ZCT0w5L0REK1JwSkpyY1Exa2IKL0J1U294OFZMeDZFemNBVEErbDFMQmREL2c3WlFvbUQzVFZQM2c0bjVWZXlBK2FBSDV0NFhNRDVPY1AyYllyKwpqTjBwc0tVKyt4bExVUTZNWG9CUllSN25UZXlOSFUyTTlEejR6VkNCVGlUbEZOMFhSMWc9Ci0tLS0tRU5EIFJTQSBQUklWQVRFIEtFWS0tLS0tCg=="), management))

    verify(management)
  }

}
