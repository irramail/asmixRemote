package com.example

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers.`Content-Type`
import java.io.FileNotFoundException
import java.io.IOException
import java.io.File
import scala.io.Source

import sys.process._

import scala.concurrent.Future
import com.example.UserRegistryActor._
import akka.pattern.ask
import akka.util.Timeout

//#user-routes-class
trait UserRoutes extends JsonSupport {
  val filenameRm = System.getProperty("user.home") + "/.rm"
  val tagEQ = "EQ:YES"
  val rmLinesEq = try {
    Source.fromFile(filenameRm).getLines.toArray filter (_.mkString == tagEQ)
  } catch {
    case e: Exception => Array[String]()
  }

  val eqYes = rmLinesEq.length match {
    case 0 => "# "
    case _ => "  "
  }

  printToFile(new File("/tmp/eqPipe.sh")) { p =>
    p.println(eqYes + "BG=`ps aux | grep bgmpfifo | grep mplayer | wc -l`")
    p.println(eqYes + "test 0 -eq \"$BG\" && echo 'af equalizer='`cat ~/eq` > /home/uid0001/mpfifo || echo 'af equalizer='`cat ~/eq` > /home/uid0001/bgmpfifo")
    p.println("echo '<RUNTIME>'`date --rfc-3339=ns | head -c 19`'</RUNTIME><TEXT>Изменение локальных настроек: EQ: '`cat ~/eq`', MUSIC_VOL: '`cat ~/task/MUSIC_VOL`', JINGL_VOL: '`cat ~/task/JINGL_VOL`'</TEXT>' > ~/task/REPORTED/asmixRemote")
  }
  val resultEqSh = Process("chmod +x /tmp/eqPipe.sh").!!

  val filenameEq = System.getProperty("user.home") + "/eq"

  val filenameVolsOfDay = System.getProperty("user.home") + "/task/VOLSOFDAY"

  val filenameMusic: String = System.getProperty("user.home") + "/task/MUSIC_VOL"
  val filenameTmpMusic: String = "/tmp/MUSIC_VOL"
  val filenameTmpNewMusic: String = "/tmp/uid0001_task_MUSIC_VOL"

  val filenameJingl: String = System.getProperty("user.home") + "/task/JINGL_VOL"
  val filenameTmpJingl: String = "/tmp/JINGL_VOL"
  val filenameTmpNewJingl: String = "/tmp/uid0001_task_JINGL_VOL"

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }

  def getVol(filename: String, defaultValue: String): String = try {
    Source.fromFile(filename).getLines.mkString
  } catch {
    case e: Exception => defaultValue
  }

  val tmpMusicVol = getVol(filenameMusic, "0:0:0").split(":")
  val musicVol = if (tmpMusicVol.length == 3) tmpMusicVol else Array.fill(3)("0")

  val tmpJinglVol = getVol(filenameJingl, "0:0:0").split(":")
  val jinglVol = if (tmpJinglVol.length == 3) tmpJinglVol else Array.fill(3)("0")

  val volsOfDayLines = try {
    Source.fromFile(filenameVolsOfDay).getLines.toArray filter (_.contains(";"))
  } catch {
    case e: Exception => (0 to 23).toArray.map("%02d".format(_) + ":00:00;0")
  }
  val volsOfDay = (volsOfDayLines map (tv => ", " + tv.split(";")(1))).mkString

  val eq: String = getVol(filenameEq, "0:0:0:0:0:0:0:0:0:0").replace(':', ',')

  //#user-routes-class

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[UserRoutes])

  // other dependencies that UserRoutes use
  def userRegistryActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  //#all-routes
  //#users-get-post
  //#users-get-delete
  lazy val userRoutes: Route =
    pathPrefix("") {
      concat(
        //#users-get-delete
        pathEnd {
          concat(
            get {
              /*              val users: Future[Users] =
                (userRegistryActor ? GetUsers).mapTo[Users]
*/
              val vol: String = musicVol(0) + ", " + musicVol(1) + ", " + musicVol(2) + ", " + jinglVol(1) + ", " + jinglVol(2) + volsOfDay + ", " + eq
              val labelVol: String = "'Master', 'Фон Л', 'Фон П', 'Рек Л', 'Рек П', '00:00', '01:00', '02:00', '03:00', '04:00', '05:00', '06:00', '07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00', '23:00', '31,25', '62,50', '125 Hz', '250 Hz', '500 Hz', '1 KHz', '2 KHz', '4 KHz', '8 KHz', '16 KHz'"
              val html: String = "<html><head><title>AsmixRemote</title><style>input[type=range] { -webkit-appearance: slider-vertical; width: 20px; height: 175px; padding: 0 5px; } div{display:inline-block;} .rotate {margin-top: 25; margin-left: -20; /* FF3.5+ */ -moz-transform: rotate(-90.0deg); /* Opera 10.5 */ -o-transform: rotate(-90.0deg); /* Saf3.1+, Chrome */ -webkit-transform: rotate(-90.0deg); /* IE6,IE7 */ filter: progid: DXImageTransform.Microsoft.BasicImage(rotation=0.083); /* IE8 */ -ms-filter: \"progid:DXImageTransform.Microsoft.BasicImage(rotation=0.083)\"; /* Standard */ transform: rotate(-90.0deg); }</style>\n</head><body><div style=\"display:block;\"><label for=\"userPassword\">Пароль: </label><input id=\"userPassword\" type=\"password\"></div><div id=\"main\"></div><div style=\"display:inline-block;\"></div></div><script>var labelVol = [" + labelVol + "]; var dsTimer = {}; function setVol(e) { var cid = e.getAttribute('id'); if ( dsTimer[cid] !== undefined ) {clearTimeout( dsTimer[cid] );} dsTimer[cid] = setTimeout(function(){ dsTimer[cid] = undefined; var xhr = new XMLHttpRequest(); xhr.open(\"POST\", \"\"); xhr.setRequestHeader(\"Content-Type\", \"application/json\"); xhr.onreadystatechange = function () { var DONE = 4; var OK = 200; if (xhr.readyState === DONE) { if (xhr.status === OK) { for(var i = 0; i < 39; i++){ var responseSplit =  xhr.responseText.substring(8).replace('\"}', ' ').split(':'); document.getElementById('svol' + i).innerHTML = responseSplit[i]} } } }; var volValues=''; for(var i = 0; i < 39; i++){ \n volValues += document.getElementById('vol' + i).value + ':';}; xhr.send('{\"name\": \"' + volValues.substring(0, volValues.length-1) + '\", \"age\": ' + document.getElementById('vol0').value + ', \"pass\": \"' + document.getElementById('userPassword').value + '\"}'); }, 500); }; window.onload = function(){ var vol = [" + vol + "]; var min = 0; var max = 100; var readOnly = 'disabled'; var container = document.getElementById(\"main\"); for(var i = 0; i < vol.length; i++){ var q = document.createElement('div'); q.innerHTML ='<span class=\"rotate\" style=\"display: block;\">' + labelVol[i] + '</span><input  type=\"range\" oninput=\"setVol(this)\" id=\"vol' + i + '\" name=\"vol' + i + '\" min=\"' + min + '\" max=\"' + max + '\" value=\"' + vol[i] + '\" ' + readOnly + '><span id=\"svol' + i + '\" style=\"display: block;\">' + vol[i] + '</span>'; container.appendChild(q); if ( i == 0 || i == 4 || i == 28){var tail = '<div style=\"margin:0 10\"></div>'; if (i==28) {min = -12; max = 12;}; if (i==4){readOnly = ''}; if ( i==28 && '" + eqYes + "' == '# '){ readOnly = 'disabled' }; var tailElement = document.createElement('div'); tailElement.innerHTML = tail; container.appendChild(tailElement);}; } document.getElementById('vol0').disabled = false; }; if ( '" + eqYes + "' == '# '){ document.body.innerHTML += '<div>Для управления эквалайзером добавьте EQ:YES в .rm<div>'; }</script></body></html>"

              complete(HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, html)))
            },
            post {
              entity(as[User]) { user =>
                val userCreated: Future[ActionPerformed] =
                  (userRegistryActor ? CreateUser(user)).mapTo[ActionPerformed]
                onSuccess(userCreated) { performed =>
                  val fPass = try { Source.fromFile(System.getProperty("user.home") + "/web_pass").getLines.mkString } catch { case e: Exception => "" }
                  val rmLinesIdPc = try { Source.fromFile(System.getProperty("user.home") + "/.rm").getLines.toArray filter (_.startsWith("ID:")) } catch { case e: Exception => Array("ID:0000") }
                  val idPc = rmLinesIdPc.length match {
                    case 0 => "0000"
                    case _ => rmLinesIdPc(0).split(":")(1)
                  }

                  if (fPass + idPc == user.pass) {
                    val volSplit = performed.vol.split(":")

                    printToFile(new File(filenameMusic)) { p =>
                      p.print(volSplit(0) + ":" + volSplit(1) + ":" + volSplit(2))
                    }
                    printToFile(new File(filenameTmpMusic)) { p =>
                      p.print(volSplit(0) + ":" + volSplit(1) + ":" + volSplit(2))
                    }
                    printToFile(new File(filenameTmpNewMusic)) { p =>
                      p.print(volSplit(0) + ":" + volSplit(1) + ":" + volSplit(2))
                    }

                    printToFile(new File(filenameJingl)) { p =>
                      p.print(volSplit(0) + ":" + volSplit(3) + ":" + volSplit(4))
                    }
                    printToFile(new File(filenameTmpJingl)) { p =>
                      p.print(volSplit(0) + ":" + volSplit(3) + ":" + volSplit(4))
                    }
                    printToFile(new File(filenameTmpNewJingl)) { p =>
                      p.print(volSplit(0) + ":" + volSplit(3) + ":" + volSplit(4))
                    }

                    printToFile(new File(filenameVolsOfDay)) { p =>
                      for { x <- 0 to 23 } p.println("%02d".format(x) + ":00:00;" + volSplit(x + 5))
                    }

                    printToFile(new File(filenameEq)) { p =>
                      p.print((volSplit.slice(29, volSplit.length) map (tv => tv + ":")).mkString.dropRight(1))
                    }

                    val resultEqSh = Process("/tmp/eqPipe.sh").!!

                  }

                  //log.info("Created user [{}]: {}", user.name, performed.vol)
                  complete((StatusCodes.OK, performed))
                }
              }
            })
        }
      //#users-get-post
      //#users-get-delete
      /*
        path(Segment) { name =>
          concat(
            get {
              //#retrieve-user-info
              val maybeUser: Future[Option[User]] =
                (userRegistryActor ? GetUser(name)).mapTo[Option[User]]
              rejectEmptyResponse {
                complete(maybeUser)
              }
              //#retrieve-user-info
            },
            delete {
              //#users-delete-logic
              val userDeleted: Future[ActionPerformed] =
                (userRegistryActor ? DeleteUser(name)).mapTo[ActionPerformed]
              onSuccess(userDeleted) { performed =>
                //log.info("Deleted user [{}]: {}", name, performed.description)
                complete((StatusCodes.OK, performed))
              }
              //#users-delete-logic
            })
        }*/ )
      //#users-get-delete
    }
  //#all-routes
}
