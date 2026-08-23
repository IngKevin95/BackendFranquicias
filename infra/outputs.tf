output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "db_endpoint" {
  value = aws_db_instance.postgres.address
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.this.name
}
