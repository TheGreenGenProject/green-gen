package org.greengen.http

import org.http4s.Method.{DELETE, GET, POST, PUT}
import org.http4s.client.dsl.io._
import org.http4s.{Uri, UrlForm}

object HttpHelper {

  def get(uri: Uri, parameters: (String, Any)*) =
    GET(urlForm(parameters:_*), uri)

  def post(uri: Uri, parameters: (String, Any)*) =
    POST(urlForm(parameters:_*), uri)

  def delete(uri: Uri, parameters: (String, Any)*) =
    DELETE(urlForm(parameters:_*), uri)

  def put(uri: Uri, parameters: (String, Any)*) =
    PUT(urlForm(parameters:_*), uri)

  def form(uri: Uri, parameters: (String, Any)*) =
    POST(urlForm(parameters:_*), uri)

  def urlForm(parameters: (String, Any)*) =
    UrlForm(parameters.map { case (k,v) => k -> v.toString }:_*)

}
